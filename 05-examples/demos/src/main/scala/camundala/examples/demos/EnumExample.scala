package camundala.examples.demos

import camundala.bpmn.*
import camundala.domain.*

object EnumExample extends BpmnDsl:

  enum Input derives ConfiguredCodec:

    case A(
        someValue: Option[String] = Some("hello"),
        simpleEnum: SimpleEnum = SimpleEnum.One,
        customMock: Option[Output] = Some(Output.A())
    )
  object Input:
    given ApiSchema[Input] = deriveApiSchema

  enum Output:

    case A(someOut: Option[String] = Some("hello"), intValue: Int = 12, simpleEnum: SimpleEnum = SimpleEnum.One)
  object Output:
    given ApiSchema[Output] = deriveApiSchema
    given InOutCodec[Output] = deriveInOutCodec

  enum SimpleEnum derives ConfiguredEnumCodec:
     case One,Two

  object SimpleEnum:
    given ApiSchema[SimpleEnum] = deriveEnumSchema

  lazy val enumExample = process(
    "enum-example",
    in = Input.A(),
    out = Output.A()
  )

end EnumExample
