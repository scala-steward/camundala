package camundala
package camunda

import bpmn.*
import camundala.camunda.CamundaMapperMacros.mapImpl
import org.camunda.bpm.model.bpmn.{BpmnModelInstance, Bpmn as CBpmn}
import io.circe.generic.auto.*
import org.camunda.bpm.model.bpmn.builder.{
  AbstractFlowNodeBuilder,
  CallActivityBuilder
}
import org.camunda.bpm.model.bpmn.instance.camunda.{
  CamundaIn,
  CamundaInputOutput,
  CamundaOut,
  CamundaOutputParameter,
  CamundaScript
}
import sttp.tapir.generic.auto.*

import java.io.File
import scala.language.implicitConversions

trait GenerateCamundaBpmn extends BpmnDsl, ProjectPaths, App:

  def run(generateBpmns: Bpmn*): Unit =
    for bpmn <- generateBpmns
    yield
      println(s"// WITH IDS: ${bpmn.path}")
      val modelInstance = toCamunda(
        bpmn
      )
      println(s"// Generated: ${(generatedPath / bpmn.path.toIO.getName).toIO}")
      CBpmn.writeModelToFile(
        (generatedPath / bpmn.path.toIO.getName).toIO,
        modelInstance
      )

  private def toCamunda(
      bpmn: Bpmn
  ): BpmnModelInstance =
    implicit val modelInstance: BpmnModelInstance =
      CBpmn.readModelFromFile(bpmn.path.toIO)

    bpmn.processes.foreach(_.toCamunda)
    modelInstance

  extension (process: Process[?, ?])
    def bpmn =
      BpmnProcess(process)

  extension (inOut: InOut[?, ?, ?])
    implicit def toBpmn: BpmnInOut =
      BpmnInOut(inOut)

  extension [In <: Product, Out <: Product](ca: CallActivity[In, Out])

    inline def mapOut[A](inline path: Out => A, targetName: String) =
      ${ mapImpl('{BpmnInOut(ca)}, 'path, 'targetName) }


  extension (bpmnProcess: BpmnProcess)

    def toCamunda: FromCamundable[Unit] =
      val cProc: CProcess = summon[CBpmnModelInstance]
        .getModelElementById(bpmnProcess.id)
      bpmnProcess.elements.collect {
        case ca @ BpmnInOut(_: CallActivity[?, ?], _, _) =>
          mergeCallActivity(ca)
      }

    def mergeCallActivity(ca: BpmnInOut): FromCamundable[Unit] =
      println(s"INOUT: $ca")

      val cCA: CCallActivity = summon[CBpmnModelInstance]
        .getModelElementById(ca.id)
      println(s"CallActivity: $ca")
      println(s"cCA: $cCA")
      merge(cCA)

      def merge(elem: CCallActivity): FromCamundable[Unit] =
        val builder
            : AbstractFlowNodeBuilder[CallActivityBuilder, CCallActivity] =
          elem.builder()
        def mergeIn(p: Product): FromCamundable[Unit] =
          p.productElementNames.foreach { v =>
            val param: CamundaIn =
              summon[CBpmnModelInstance].newInstance(classOf[CamundaIn])
            param.setCamundaSource(v)
            param.setCamundaTarget(v)
            builder.addExtensionElement(param)
          }

        def mergeOut(p: Product): FromCamundable[Unit] =
          p.productElementNames.foreach { v =>
            val param: CamundaOut =
              summon[CBpmnModelInstance].newInstance(classOf[CamundaOut])
            param.setCamundaSource(v)
            param.setCamundaTarget(v)
            builder.addExtensionElement(param)
          }

        println(s"TT ${ca.inOut.out}")
        mergeOutputParams(builder, ca.outMappers)
        ca.inOut.in match
          case p: Product =>
            mergeIn(p)
        ca.inOut.out match
          case p: Product =>
            println(s"TT $p")
            mergeOut(p)

  def mergeOutputParams(
      builder: AbstractFlowNodeBuilder[?, ?],
      mappers: Seq[PathMapper]
  ): FromCamundable[Unit] =
    val inout =
      summon[CBpmnModelInstance].newInstance(classOf[CamundaInputOutput])
    builder.addExtensionElement(inout)
    mappers
      .foreach { case pm @ PathMapper(varName, _, _) =>
        val cp = summon[CBpmnModelInstance].newInstance(
          classOf[CamundaOutputParameter]
        )
        cp.setCamundaName(varName)
        inout.getCamundaOutputParameters.add(cp)
        val script: CamundaScript =
          summon[CBpmnModelInstance].newInstance(classOf[CamundaScript])
        script.setCamundaScriptFormat("Groovy")
        script.setTextContent(pm.printGroovy())
        cp.setValue(script)
      }
end GenerateCamundaBpmn
