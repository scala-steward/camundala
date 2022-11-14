package camundala
package simulation

import camundala.bpmn.*

import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions

trait SimulationDsl extends GatlingSimulation, TestOverrideExtensions:

  class SimulationBuilder:
    private val ib = ListBuffer.empty[SScenario]

    def pushScenario(x: SScenario) = ib.append(x)

    def mkBlock = SSimulation(ib.toList)

  type SimulationConstr = SimulationBuilder ?=> Unit

  extension (scen: SScenario)
    private[simulation] def stage: SimulationConstr =
      (bldr: SimulationBuilder) ?=> bldr.pushScenario(scen)
  /*
  class ScenarioBuilder(name: String):
    private val ib = ListBuffer.empty[CStep]

    def pushStep(x: CStep) =
      ib.append(x)

    def mkBlock(process: Process[_, _]) = CScenario(name, process, ib.toList)

  type ScenarioConstr = ScenarioBuilder ?=> Unit

  extension (ut: UserTask[_, _]) private[simulation] def stage: ScenarioConstr =
    (bldr: ScenarioBuilder) ?=> bldr.pushStep(CUserTask(ut.id, ut))
  extension (ut: CUserTask) private[simulation] def stage: ScenarioConstr =
    (bldr: ScenarioBuilder) ?=> bldr.pushStep(ut)

  inline def scenario(inline process: Process[_, _])(body: ScenarioConstr): SimulationConstr =
    val sb = ScenarioBuilder(nameOfVariable(process))
    body(using sb)
    val scen = sb.mkBlock(process).stage
    println(s"SCENARIO: $scen")
    scen

  extension(activity: Activity[_,_,_])

    @targetName("step")
    def unary_+ : ScenarioConstr =
      activity match
        case ut: UserTask[_,_] => CUserTask(nameOfVariable(activity), ut).stage

   */
  def simulate(body: SimulationConstr): Unit =
    val sb = SimulationBuilder()
    body(using sb)
    val sim = sb.mkBlock
    run(sim) // runs Gatling Load Tests

  def scenario(scen: ProcessScenario): SimulationConstr =
    scenario(scen)()

  def scenario(scen: DmnScenario): SimulationConstr =
    scen.stage

  def scenario(scen: ProcessScenario)(body: SStep*): SimulationConstr =
    scen.copy(steps = body.toList).stage

  def ignore(scen: ProcessScenario)(body: SStep*): SimulationConstr =
    scen.copy(steps = body.toList).ignored.stage

  inline def badScenario(
      inline process: Process[_, _],
      status: Int,
      errorMsg: Option[String] = None
  ): SimulationConstr =
    BadScenario(nameOfVariable(process), process, status, errorMsg).stage

  inline def incidentScenario(
                               inline process: Process[_, _],
                               incidentMsg: String
  ): SimulationConstr =
    IncidentScenario(nameOfVariable(process), process, incidentMsg).stage

  inline def subProcess(inline process: Process[_, _])(
      body: SStep*
  ): SSubProcess =
    SSubProcess(nameOfVariable(process), process, body.toList)

  implicit inline def toScenario(
      inline process: Process[_, _]
  ): ProcessScenario =
    ProcessScenario(nameOfVariable(process), process)

  implicit inline def toScenario(
                                  inline decisionDmn: DecisionDmn[_, _]
                                ): DmnScenario =
    DmnScenario(nameOfVariable(decisionDmn), decisionDmn)

  implicit inline def toStep(inline inOut: UserTask[_, _]): SUserTask =
    SUserTask(nameOfVariable(inOut), inOut)
  implicit inline def toStep(inline inOut: ReceiveMessageEvent[_]): SReceiveMessageEvent =
    SReceiveMessageEvent(nameOfVariable(inOut), inOut)
  implicit inline def toStep(inline inOut: ReceiveSignalEvent[_]): SReceiveSignalEvent =
    SReceiveSignalEvent(nameOfVariable(inOut), inOut)

  extension (rse: ReceiveSignalEvent[_])
    def waitFor(readyVariable: String, readyValue: Any = true) =
      SReceiveSignalEvent(rse.name, rse, readyVariable, readyValue)

  end extension

  extension (rme: ReceiveMessageEvent[_])
    def waitFor(readyVariable: String, readyValue: Any): SReceiveMessageEvent =
      SReceiveMessageEvent(rme.name, rme, Some(readyVariable), readyValue)
    def start: SReceiveMessageEvent =
      SReceiveMessageEvent(rme.name, rme).start
  end extension

  def waitFor(timeInSec: Int): SWaitTime = SWaitTime(timeInSec)
