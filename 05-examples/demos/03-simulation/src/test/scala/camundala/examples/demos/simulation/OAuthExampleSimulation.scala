package camundala.examples.demos.simulation

import camundala.examples.demos.bpmn.TestDomain.CamundalaGenerateTestP
import camundala.simulation.*
import camundala.simulation.custom.*

/** exampleDemos/It/testOnly *OAuthExampleSimulation BE AWARE - for this must run a local Docker
  * with FSSO setup. no process is needed.
  */
class OAuthExampleSimulation extends OAuthSimulationDsl:

  simulate (
    ignore.badScenario(
      CamundalaGenerateTestP,
      404,
      "No matching process definition with key: camundala-generate-test and tenant-id: 0949"
    )
  )

  override implicit def config =
    super.config
      .withTenantId("0949")

  lazy val fsso: Fsso = Fsso(
    s"http://kubernetes.docker.internal:8090/auth/realms/${config.tenantId.get}/protocol/openid-connect",
    Map(
      "grant_type" -> "password",
      "client_id" -> "all",
      "client_secret" -> "6ec0e8ce-eff1-456f-bc2f-907b6fcb5157",
      "username" -> "admin",
      "password" -> "admin",
      "scope" -> "fcs"
    )
  )
end OAuthExampleSimulation
