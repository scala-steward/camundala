package camundala
package examples.invoice

import camundala.bpmn.ServiceProcess
import camundala.examples.invoice.ArchiveInvoice.{In, Out, ServiceIn, ServiceOut, serviceMock, serviceName, serviceProcess}
import domain.*

object StarWarsRestApi:

  final val serviceName = "star-wars-api-people-detail"
  type ServiceIn = NoInput
  type ServiceOut = People
  lazy val serviceMock: ServiceOut = People()
  
  @description("Same Input as _InvoiceReceipt_, only different Mocking")
  case class In(
      id: Int = 1
  )

  object In:
    given Schema[In] = Schema.derived
    given CirceCodec[In] = deriveCodec
  end In

  case class Out(
      people: People = People()
  )

  object Out:
    given Schema[Out] = Schema.derived
    given CirceCodec[Out] = deriveCodec
  end Out
  
  case class People(
      name: String = "Luke Skywalker",
      height: String = "172",
      mass: String = "77",
      hair_color: String = "blond",
      skin_color: String = "fair",
      eye_color: String = "blue"
  )
  object People:
    given Schema[People] = Schema.derived
    given CirceCodec[People] = deriveCodec
  end People

  lazy val example: ServiceProcess[In, Out, ServiceIn, ServiceOut] =
    serviceProcess(
      serviceName,
      descr = "Get People Details from StarWars API",
      in = In(),
      out = Out(),
      defaultServiceMock = serviceMock,
    )
end StarWarsRestApi