package pme123.camundala.cli

import cats.effect.ExitCode
import com.monovore.decline._
import com.monovore.decline.effect.CommandIOApp
import pme123.camundala.app.appRunner
import pme123.camundala.app.appRunner.AppRunner
import pme123.camundala.camunda.bpmnService.BpmnService
import pme123.camundala.camunda.deploymentService.DeploymentService
import pme123.camundala.camunda.{DeployResult, bpmnService, deploymentService}
import pme123.camundala.cli.ProjectInfo._
import pme123.camundala.model.bpmn.CamundalaException
import pme123.camundala.model.register.deployRegister.DeployRegister
import pme123.camundala.model.deploy.{Deploy, DeployId, DockerConfig}
import pme123.camundala.model.register.deployRegister
import pme123.camundala.services.dockerComposer
import pme123.camundala.services.dockerComposer.DockerComposer
import zio._
import zio.clock.Clock
import zio.console.{Console, putStr => p, putStrLn => pl}
import zio.interop.catz._

import scala.io.{BufferedSource, Source}

object cliApp {

  type CliApp = Has[Service]

  trait Service {
    def run(projectInfo: ProjectInfo): ZIO[Console, Throwable, Nothing]
  }

  def run(projectInfo: ProjectInfo): ZIO[CliApp with Console, Throwable, Nothing] =
    ZIO.accessM(_.get.run(projectInfo))

  type CliAppDeps = Clock with Console with BpmnService with DeployRegister with DeploymentService with DockerComposer with AppRunner

  lazy val live: URLayer[CliAppDeps, CliApp] =
    ZLayer.fromServices[Clock.Service, Console.Service, bpmnService.Service, deployRegister.Service, deploymentService.Service, dockerComposer.Service, appRunner.Service, Service] {
      (clock, console, bpmnService, deployReg, deployService, dockerService, appRunner) =>

        import CliCommand._

        lazy val command = Command[Task[ExitCode]]("", "CLI for Camunda")(
          (validateBpmnOpts orElse deployBpmnOpts orElse undeployBpmnOpts orElse deploymentsOpts orElse dockerCommand orElse appCommand).map {
            case ValidateBpmn(bpmnId) =>
              validateBpmn(bpmnId)
            case DeployBpmn(deployId) =>
              deployBpmn(deployId)
            case UndeployBpmn(deployId) =>
              undeployBpmn(deployId)
            case Deployments() =>
              deployments()
            case other: UIO[ExitCode] =>
              other
          }
        )

        lazy val dockerCommand: Opts[ZIO[Any, Nothing, ExitCode]] =
          Opts.subcommand("docker", "Work with Docker Compose") {
            (dockerUpOpts orElse dockerStopOpts orElse dockerDownOpts).map {
              case Docker.Up(bpmnId) =>
                dockerUp(bpmnId)
              case Docker.Stop(bpmnId) =>
                dockerStop(bpmnId)
              case Docker.Down(bpmnId) =>
                dockerDown(bpmnId)
            }
          }

        lazy val appCommand: Opts[ZIO[Any, Nothing, ExitCode]] =
          Opts.subcommand("app", "Manage your App") {
            (appStartOpts orElse appStopOpts orElse appRestartOpts).map {
              case App.Start() =>
                appStart()
              case App.Stop() =>
                appStop()
              case App.Restart() =>
                appRestart()
            }
          }

        def validateBpmn(bpmnId: DeployId) = {
          (for {
            _ <- appRunner.update()
            valWarns <- bpmnService.validateBpmn(bpmnId)
            result <- printSuccess(s"Successful validated BPMN '$bpmnId'${scala.Console.YELLOW}\nWarnings:",
              valWarns.value.map(_.msg).mkString(" - ", "\n - ", ""))
          } yield result)
            .catchAll(printError(_, "Validation failed:"))
        }

        def deployBpmn(deployId: DeployId) =
          runWithDeployId[Seq[DeployResult]](deployId, " Deploy BPMN", config => Task.foreach(config.bpmns)(b => deployService.deploy(b)), results =>
              results.foldLeft("")((r, dr) => s"$r\n${scala.Console.YELLOW}- Warnings ${dr.name}:\n${dr.validateWarnings.value.mkString(" - ", "\n - ", "")}"))

        def undeployBpmn(deployId: DeployId) =
          runWithDeployId[List[Unit]](deployId, " Undeploy BPMN", config => Task.foreach(config.bpmns)(b => deployService.undeploy(b)), _ => "")

        def deployments() =
          runUnit("Get Deployments", () => deployService.deployments().map(_.mkString("\n")))

        def appStart() =
          runUnit("Start App", () => appRunner.start().map(_ => ""))
        def appStop() =
          runUnit("Stop App", () => appRunner.stop().map(_ => ""))
        def appRestart() =
          runUnit("Restart App", () => appRunner.restart().map(_ => ""))

        def dockerUp(deployId: DeployId) =
          runDocker(deployId, "Docker Up", dockerService.runDockerUp(_).provideLayer(ZLayer.succeed(clock)))

        def dockerStop(deployId: DeployId) =
          runDocker(deployId, "Docker Stop", dockerService.runDockerStop)

        def dockerDown(deployId: DeployId) =
          runDocker(deployId, "Docker Down", dockerService.runDockerDown)

        def runDocker(deployId: DeployId, label: String, run: DockerConfig => Task[String]) =
          runWithDeployId[String](deployId, label, deploy => run(deploy.dockerConfig), str => str)

        def runUnit(label: String, run: () => Task[String]) = {
          (for {
            results <- run()
            result <- printSuccess(s"Successful $label", results)
          } yield result)
            .catchAll(printError(_, s"$label failed:"))
        }

        def runWithDeployId[T](deployId: DeployId, label: String, run: Deploy => Task[T], details: T => String) = {
          (for {
            maybeDeploy <- deployReg.requestDeploy(deployId)
            results <-
              if (maybeDeploy.isEmpty)
                ZIO.fail(CliAppException(s"There is no Deployment with the id '$deployId''"))
              else
                run(maybeDeploy.get)
            result <- printSuccess(s"Successful $label", details(results))
          } yield result)
            .catchAll(printError(_, s"$label failed"))
        }

        def printSuccess(msg: String, details: String): UIO[ExitCode] = {
          console.putStrLn(s"${scala.Console.GREEN}$msg") *>
            console.putStrLn(details) *>
            console.putStr(scala.Console.RESET) *>
            ZIO.succeed(ExitCode.Success)
        }

        def printError(e: Throwable, msg: String): UIO[ExitCode] = {
          console.putStrLn(s"${scala.Console.RED}$msg") *>
            ZIO.succeed(e.printStackTrace()) *>
            console.putStr(scala.Console.RESET) *>
            ZIO.succeed(ExitCode.Error)
        }

        lazy val cliRunner =
          (for {
            input <- console.getStrLn
            _ <- CommandIOApp.run(command, input.split(" ").toList)
          } yield ())
            .tapError(e => console.putStrLn(s"Error: $e"))
            .forever

        (projectInfo: ProjectInfo) =>
          for {
            _ <- intro *> printProject(projectInfo)
            d <- cliRunner
          } yield d

    }

  private val width = 84
  private val versionFile: zio.Managed[Throwable, BufferedSource] = zio.Managed.make(ZIO.effect(Source.fromFile("./version")))(s => ZIO.succeed(s.close()))

  private[cli] val camundala: Task[ProjectInfo] =
    for {
      version <- versionFile.use(vf => ZIO.effect(vf.getLines().next()))
    } yield ProjectInfo("camundala", "pme123", version, "https://github.com/pme123/camundala")

  private val intro =
    for {
      _ <- p(scala.Console.MAGENTA)
      _ <- pl("*" * width)
      _ <- p(scala.Console.BLUE)
      _ <- p(
        """|     _____
           |  __|___  |__  ____    ____    __  __   _  ____   _  _____   ____    ____    ____
           | |   ___|    ||    \  |    \  /  ||  | | ||    \ | ||     \ |    \  |    |  |    \
           | |   |__     ||     \ |     \/   ||  |_| ||     \| ||      \|     \ |    |_ |     \
           | |______|  __||__|\__\|__/\__/|__||______||__/\____||______/|__|\__\|______||__|\__\
           |    |_____|    Doing Camunda with Scala""".stripMargin)
      _ <- p(scala.Console.MAGENTA)
      pi <- camundala
      _ <- line(versionLabel, pi.version, leftAligned = false)
      _ <- line(licenseLabel, pi.license, leftAligned = false)
      _ <- line("", pi.sourceUrl, leftAligned = false)
      _ <- pl("")
      _ <- pl("*" * width)
      _ <- pl(" For Help type '--help'")
      _ <- p(scala.Console.RESET)
    } yield ()

  private def printProject(projectInfo: ProjectInfo) = {
    for {
      _ <- p(scala.Console.BLUE)
      _ <- line(nameLabel, projectInfo.name)
      _ <- line(orgLabel, projectInfo.org)
      _ <- line(versionLabel, projectInfo.version)
      _ <- line(licenseLabel, projectInfo.license)
      _ <- line(sourceUrlLabel, projectInfo.sourceUrl)
      _ <- pl("")
      _ <- pl("-" * width)
      _ <- p(scala.Console.RESET)
    } yield ()
  }

  private def line(label: String, value: String, leftAligned: Boolean = true) =
    for {
      _ <- pl("")
      _ <- if (leftAligned)
        p(" ")
      else
        p(" " * (width - (value.length + label.length)))
      _ <- p(s"$label$value")
    } yield ()

  case class CliAppException(msg: String) extends CamundalaException

}