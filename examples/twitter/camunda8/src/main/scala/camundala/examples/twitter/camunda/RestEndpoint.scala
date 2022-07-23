package camundala.examples.twitter.camunda

import camundala.bpmn.*
import camundala.examples.twitter.api.Tweet
import cats.Show
import io.camunda.zeebe.client.ZeebeClient
import io.camunda.zeebe.client.api.command.CreateProcessInstanceCommandStep1.CreateProcessInstanceCommandStep3
import io.camunda.zeebe.client.api.command.FinalCommandStep
import io.camunda.zeebe.client.api.response.{ProcessInstanceEvent, ProcessInstanceResult}
import io.circe.{DecodingFailure, HCursor}
import io.circe.syntax.EncoderOps
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, ResponseEntity}
import org.springframework.web.bind.annotation.{PutMapping, RequestBody}
import scala.jdk.CollectionConverters.*

trait RestEndpoint extends Validator:

  type Response = ResponseEntity[ProcessInstanceEvent | ProcessInstanceResult | String]

  @Autowired
  protected var zeebeClient: ZeebeClient = _

  def createInstance[In : Decoder, Out <: Product: Decoder](
      processId: String,
      startVars: Either[String, CreateProcessInstanceIn[In, Out]]
  ): Response =
    (for
      startObj <- startVars
      process <- start(processId, startObj)
    yield process) match
      case Right(process: ProcessInstanceEvent) =>
        ResponseEntity
          .status(HttpStatus.OK)
          .body(process)
      case Right(process: ProcessInstanceResult) =>
        ResponseEntity
          .status(HttpStatus.OK)
          .body(process)
      case Left(errorMsg) =>
        ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body(errorMsg.toString)

  private def start[In : Decoder, Out <: Product: Decoder](
      processId: String,
      startObj: CreateProcessInstanceIn[In, Out]
  ): Either[String, ProcessInstanceEvent | ProcessInstanceResult] =
    try {
      val command =
        zeebeClient.newCreateInstanceCommand
          .bpmnProcessId(processId)
          .latestVersion
          .variables(startObj.variables)
      val endCommand =
        if (startObj.fetchVariables.isEmpty)
          command
        else {
          val fetchedVariables = startObj.fetchVariables.get.getDeclaredFields.map(_.getName).toList.asJava
          println(s"fetchedVariables: $fetchedVariables")
          command
            .withResult()
            .fetchVariables(fetchedVariables)
        }
      Right(endCommand.send.join)
    } catch {
      case ex: Throwable =>
        ex.printStackTrace()
        Left(s"Problem starting the Process: ${ex.getMessage}")
    }

