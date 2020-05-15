package pme123.camundala.examples.twitter

import eu.timepit.refined.auto._
import pme123.camundala.app.appRunner
import pme123.camundala.model.ModelLayers
import pme123.camundala.model.bpmn.Url
import zio.console.Console
import zio.test.Assertion._
import zio.test._

object TwitterAppSuite extends DefaultRunnableSpec {

  val url: Url = "http://localhost:8080/camunda"

  def spec: ZSpec[environment.TestEnvironment, Any] =
    suite("TwitterAppSuite")(
      testM("compile bpmnModels") {
        for {
          result <- appRunner.update()
        } yield
          assert(result)(isUnit)
      } // Console with DeployRegister with BpmnRegister with HttpServer
    ).provideCustomLayer((ModelLayers.bpmnRegisterLayer ++ ModelLayers.deployRegisterLayer ++ TwitterApp.httpServerLayer ++ Console.live >>> TwitterApp.twitterApp).mapError(TestFailure.fail))

}
