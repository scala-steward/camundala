package pme123.camundala.camunda

import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.rest.util.EngineUtil
import pme123.camundala.app.sttpBackend
import pme123.camundala.camunda.bpmnGenerator.BpmnGenerator
import pme123.camundala.camunda.bpmnService.BpmnService
import pme123.camundala.camunda.deploymentService.DeploymentService
import pme123.camundala.camunda.httpDeployClient.HttpDeployClient
import pme123.camundala.camunda.processEngineService.ProcessEngineService
import pme123.camundala.camunda.service.restService
import pme123.camundala.camunda.service.restService.RestService
import pme123.camundala.config.ConfigLayers
import pme123.camundala.model.ModelLayers
import zio.clock.Clock
import zio.logging.Logging
import zio._

object CamundaLayers {
  private def logLayer(loggerName: String): ULayer[Logging] =
    ModelLayers.logLayer(loggerName, "pme123.camundala.camunda")

  lazy val restServicetLayer: TaskLayer[RestService] = sttpBackend.sttpBackendLayer ++ logLayer("RestService") ++ Clock.live >>> restService.live

  lazy val bpmnServiceLayer: TaskLayer[BpmnService] = ConfigLayers.appConfigLayer ++ ModelLayers.bpmnRegisterLayer >>> bpmnService.live
  lazy val httpDeployClientLayer: TaskLayer[HttpDeployClient] =
    sttpBackend.sttpBackendLayer ++ bpmnServiceLayer ++ logLayer("DockerRunner") ++ ConfigLayers.appConfigLayer ++ Clock.live >>> httpDeployClient.live
  lazy val deploymentServiceLayer: TaskLayer[DeploymentService] = bpmnServiceLayer ++ processEngineServiceLayer ++  logLayer("DeploymentService") >>> deploymentService.live
  lazy val bpmnGeneratorLayer: TaskLayer[BpmnGenerator] = logLayer("BpmnGenerator") ++ ConfigLayers.appConfigLayer >>> bpmnGenerator.live

  lazy val processEngineLayer: ZLayer[Any, Throwable, Has[() => ProcessEngine]] = // ProcessEngine must be lazy!
    ZLayer.fromAcquireRelease(ZIO.effect(() => EngineUtil.lookupProcessEngine(null)))(pe =>
      ZIO.effect(pe().close()).ignore
    )
  lazy val processEngineServiceLayer: TaskLayer[ProcessEngineService] = ConfigLayers.appConfigLayer ++ processEngineLayer >>> processEngineService.live


}