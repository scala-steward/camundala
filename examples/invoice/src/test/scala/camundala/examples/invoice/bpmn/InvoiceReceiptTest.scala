package camundala.examples.invoice.bpmn

import camundala.bpmn.*

import camundala.examples.invoice.bpmn.InvoiceApi.*
import camundala.test.*
import org.junit.Assert.{assertEquals, assertTrue}
import org.junit.Test
import io.circe.generic.auto.*
import io.circe.syntax.*
import sttp.tapir.generic.auto.*
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*

import java.util
import java.util.{HashSet, List, Set}
import scala.compiletime.{constValue, constValueTuple}
import scala.deriving.Mirror

class InvoiceReceiptTest extends ScenarioRunner :

  lazy val config: TestConfig =
    testConfig
      .deployments(
        baseResource / "invoice.v2.bpmn",
        baseResource / "invoiceBusinessDecisions.dmn",
        formResource / "approve-invoice.html",
        formResource / "prepare-bank-transfer.html",
        formResource / "start-form.html"
      )
      .registries()

  @Test
  def testInvoiceReceipt(): Unit =
    test(`Invoice Receipt`)(
      AssignApproverGroupBRT,
      checkGroupIds,
      ApproveInvoiceUT,
      PrepareBankTransferUT,
      ArchiveInvoiceST,
      InvoiceProcessedEE
    )

  @Test
  def testInvoiceReceiptWithReviewClarified(): Unit =
    mockSubProcess(`Review Invoice`)
    test(
      `Invoice Receipt`
        .withOut(InvoiceReceiptCheck(true, Some(true)))
    )(
      AssignApproverGroupBRT,
      ApproveInvoiceUT // do not approve
        .withOut(ApproveInvoice(false)),
      `Review Invoice clarified`,
      ApproveInvoiceUT, // now we approve it
      PrepareBankTransferUT,
      InvoiceProcessedEE
    )

  @Test
  def testInvoiceReceiptWithReviewNotClarified(): Unit =
    mockSubProcess(`Review Invoice not clarified`
      .withId(`Review Invoice`.id).asProcess)
    test(
      `Invoice Receipt`
        .withOut(InvoiceReceiptCheck(false, Some(false)))
    )(
      AssignApproverGroupBRT,
      ApproveInvoiceUT // do not approve
        .withOut(ApproveInvoice(false)),
      `Review Invoice not clarified`,
      InvoiceNotProcessedEE
    )

  import scala.jdk.CollectionConverters.IterableHasAsScala

  private def checkGroupIds =
    custom {
      val links = taskService.getIdentityLinksForTask(task.getId).asScala
      val approverGroups = new util.HashSet[String]
      for (link <- links) {
        approverGroups.add(link.getGroupId)
      }
      assertEquals(2, approverGroups.size)
      assertTrue(approverGroups.contains("accounting"))
      assertTrue(approverGroups.contains("sales"))
    }