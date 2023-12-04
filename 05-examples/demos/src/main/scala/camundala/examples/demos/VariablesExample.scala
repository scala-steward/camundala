package camundala.examples.demos

import camundala.bpmn.*
import camundala.domain.*

object VariablesExample extends BpmnDsl:

  case class Input(letters: Option[String] = Some("A_dynamic_2"),
                   inputVariable: DmnVariable[String] = DmnVariable("dynamic"),
                   outputVariable: DmnVariable[String] = DmnVariable("dynamicOut")
                  )

  lazy val VariablesExampleDMN = singleEntry(
    decisionDefinitionKey = "VariablesExample",
    in = Input(),
    out = "OK dynamicOut" //Camunda returns String - LocalVariablesTime.parse("2012-12-12T12:12:12")
  )


  given ApiSchema[Input] = deriveApiSchema
  given InOutCodec[Input] = deriveInOutCodec


end VariablesExample
