package camundala.examples.demos

import camundala.simulation.*
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import DecisionResultTypes.*
import scala.concurrent.duration.*

// exampleDemos/GatlingIt/testOnly *DecisionResultTypesSimulation
class DecisionResultTypesSimulation extends SimulationDsl:

  override implicit def config: SimulationConfig =
    super.config.withPort(8033)

  import TestDomain.*
  simulate {
    scenario(singleEntryDMN)
    scenario(singleResultDMN)
    scenario(collectEntriesDMN)
    scenario(resultListDMN)
    scenario(collectEntriesDMNEmptySeq)
    scenario(resultListDMNEmptySeq)
    /* bad cases
    scenario(singleResultDMNBadOutput)
    scenario(resultListDMNBadOutput)
    */
  }