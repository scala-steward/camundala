package camundala.examples.invoice.bpmn

import camundala.bpmn.*
import camundala.camunda.GenerateCamundaBpmn
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*

object InvoiceGenerateCamundaBpmnApp extends GenerateCamundaBpmn, App:

  val projectPath = pwd / "examples" / "invoice"
  import InvoiceApi.*

  run(invoiceBpmn, reviewInvoice)

  private lazy val invoiceBpmn: Bpmn = Bpmn(
    withIdPath / "invoice.v2.bpmn",
    InvoiceReceiptP
      .withElements(reviewInvoiceCA)
  )
  private lazy val reviewInvoice: Bpmn =
    Bpmn(withIdPath / "reviewInvoice.bpmn", ReviewInvoiceP)

end InvoiceGenerateCamundaBpmnApp