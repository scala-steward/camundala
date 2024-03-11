package camundala.examples.demos
package api

import camundala.api.*
import camundala.bpmn.*
import bpmn.*

// exampleDemos/run
object ProjectApiCreator extends DefaultApiCreator:
  lazy val companyName = "MyCompany"

  val projectName = "demos-example"

  protected val title = "Demos Example Process API"

  protected val version = "1.0"

  override protected val apiConfig: ApiConfig =
    ApiConfig("demoCompany")
      .withBasePath(os.pwd / "05-examples" / "demos")
      .withDocProjectUrl(project => s"https://webstor.ch/camundala/myCompany/$project")
      .withDiagramDownloadPath(
        "src/main/resources"
      )
      .withPort(8034)

  document(
    DecisionResultTypes.singleEntryDMN
      .withDiagramName("DecisionResultTypes"),
    DecisionResultTypes.collectEntriesDMN
      .withDiagramName("DecisionResultTypes"),
    DecisionResultTypes.singleResultDMN
      .withDiagramName("DecisionResultTypes"),
    DecisionResultTypes.resultListDMN
      .withDiagramName("DecisionResultTypes"),
    api(
      DecisionResultTypes.demoProcess
        .withDiagramName("mapping-example")
    )(
      DecisionResultTypes.singleEntryDMN.withDiagramName("DecisionResultTypes"),
      DecisionResultTypes.collectEntriesDMN.withDiagramName("DecisionResultTypes"),
      DecisionResultTypes.singleResultDMN.withDiagramName("DecisionResultTypes"),
      DecisionResultTypes.resultListDMN.withDiagramName("DecisionResultTypes")
    ),
    EnumExample.example,
    DateExample.DateExampleDMN,
    VariablesExample.VariablesExampleDMN,
    SimulationTestOverridesExample.simulationProcess,
    group("SignalMessageExample")(
      SignalExample.signalExample,
      MessageForExample.messageExample,
      SignalExample.signalIntermediateExample,
      MessageForExample.messageIntermediateExample
    ),
    api(TimerExample.timerProcess)(
      TimerExample.timer
    )
  )
