package camundala.api

import camundala.bpmn.*
import io.circe.*
import io.circe.syntax.*
import sttp.tapir.EndpointIO.Example
import sttp.tapir.*
import sttp.apispec.openapi.*
import sttp.tapir.json.circe.*
import sttp.apispec.openapi.circe.yaml.*

import java.text.SimpleDateFormat
import java.util.Date
import scala.reflect.ClassTag
import scala.util.matching.Regex

trait CamundaPostmanApiCreator extends PostmanApiCreator:

  protected def createPostmanForProcess(
      api: ProcessApi[?, ?],
      tag: String,
      isGroup: Boolean = false
  ): Seq[PublicEndpoint[?, Unit, ?, Any]] =
    Seq(
      api.startProcess(tag, isGroup)
    )

  protected def createPostmanForUserTask(
      api: ActivityApi[?, ?],
      tag: String
  ): Seq[PublicEndpoint[?, Unit, ?, Any]] =
    Seq(
      api.getActiveTask(tag),
      api.getTaskFormVariables(tag),
      api.completeTask(tag)
    )
  protected def createPostmanForDecisionDmn(
      api: ActivityApi[?, ?],
      tag: String
  ): Seq[PublicEndpoint[?, Unit, ?, Any]] =
    Seq(
      api.evaluateDecision(tag)
    )
  protected def createPostmanForReceiveMessageEvent(
      api: ActivityApi[?, ?],
      tag: String
  ): Seq[PublicEndpoint[?, Unit, ?, Any]] =
    Seq(
      api.correlateMessage(tag)
    )
  protected def createPostmanForReceiveSignalEvent(
      api: ActivityApi[?, ?],
      tag: String
  ): Seq[PublicEndpoint[?, Unit, ?, Any]] =
    println("SIGNAL Camunda")
    Seq(
      api.sendSignal(tag)
    )

  extension (process: ProcessApi[?, ?])

    def startProcess(tag: String, isGroup: Boolean): PublicEndpoint[?, Unit, ?, Any] =
      val path =
        tenantIdPath("process-definition" / "key" / process.endpointPath(isGroup), "start")
      val input =
        process
          .toPostmanInput((example: FormVariables) =>
            StartProcessIn(
              example,
              Some(process.name)
            )
          )
      process
        .postmanBaseEndpoint(tag, input, "StartProcess")
        .in(path)
        .post

  end extension

  extension (api: ActivityApi[?, ?])

    def getActiveTask(tag: String): PublicEndpoint[?, Unit, ?, Any] =
      val path = "task" / s"--REMOVE:${api.id}--"

      val input =
        api
          .toPostmanInput(_ => GetActiveTaskIn())
      api
        .postmanBaseEndpoint(tag, input, "GetActiveTask")
        .in(path)
        .post

    def getTaskFormVariables(tag: String): PublicEndpoint[?, Unit, ?, Any] =
      val path =
        "task" / taskIdPath() / "form-variables" / s"--REMOVE:${api.id}--"

      api
        .postmanBaseEndpoint(tag, None, "GetTaskFormVariables")
        .in(path)
        .in(
          query[String]("variableNames")
            .description(
              """A comma-separated list of variable names. Allows restricting the list of requested variables to the variable names in the list.
                |It is best practice to restrict the list of variables to the variables actually required by the form in order to minimize fetching of data. If the query parameter is ommitted all variables are fetched.
                |If the query parameter contains non-existent variable names, the variable names are ignored.""".stripMargin
            )
            .default(
              api.apiExamples.inputExamples.examples.head.productElementNames
                .mkString(",")
            )
        )
        .in(
          query[Boolean]("deserializeValues")
            .default(false)
        )
        .get

    def completeTask(tag: String): PublicEndpoint[?, Unit, ?, Any] =
      val path = "task" / taskIdPath() / "complete" / s"--REMOVE:${api.id}--"

      val input = api
        .toPostmanInput(
          (example: FormVariables) => CompleteTaskIn(example),
          api.apiExamples.outputExamples.fetchExamples
        )

      api
        .postmanBaseEndpoint(tag, input, "CompleteTask")
        .in(path)
        .post

    def evaluateDecision(tag: String): PublicEndpoint[?, Unit, ?, Any] =
      val decisionDmn = api.inOut.asInstanceOf[DecisionDmn[?, ?]]
      val path = tenantIdPath(
        "decision-definition" / "key" / definitionKeyPath(
          decisionDmn.decisionDefinitionKey
        ) / s"--REMOVE:${api.id}--",
        "evaluate"
      )
      val input = api
        .toPostmanInput((example: FormVariables) => EvaluateDecisionIn(example))
      val descr = s"""
                     |${api.descr}
                     |
                     |Decision DMN:
                     |- _decisionDefinitionKey_: `${decisionDmn.decisionDefinitionKey}`,
                     |""".stripMargin
      api
        .postmanBaseEndpoint(tag, input, "EvaluateDecision", Some(descr))
        .in(path)
        .post

    def correlateMessage(tag: String): PublicEndpoint[?, Unit, ?, Any] =
      val event = api.inOut.asInstanceOf[ReceiveMessageEvent[?]]
      val path = "message" / s"--REMOVE:${event.messageName}--"
      val input = api
        .toPostmanInput((example: FormVariables) =>
          CorrelateMessageIn(
            event.messageName,
            Some(api.name),
            tenantId = apiConfig.tenantId,
            processVariables = Some(example)
          )
        )
      val descr = s"""
                     |${api.descr}
                     |
                     |Message:
                     |- _messageName_: `${event.messageName}`,
                     |""".stripMargin
      api
        .postmanBaseEndpoint(tag, input, "CorrelateMessage", Some(descr))
        .in(path)
        .post

    def sendSignal(tag: String): PublicEndpoint[?, Unit, ?, Any] =
      val event = api.inOut.asInstanceOf[ReceiveSignalEvent[?]]
      val path = "signal" / s"--REMOVE:${event.messageName}--"
      val input = api
        .toPostmanInput((example: FormVariables) =>
          SendSignalIn(
            event.messageName,
            tenantId = apiConfig.tenantId,
            variables = Some(example)
          )
        )
      val descr = s"""
                     |${api.descr}
                     |
                     |Signal:
                     |- _messageName_: `${event.messageName}`,
                     |""".stripMargin
      api
        .postmanBaseEndpoint(tag, input, "SendSignal", Some(descr))
        .in(path)
        .post

  end extension

  extension (inOutApi: InOutApi[?, ?])

    def toPostmanInput[
        T <: Product: Encoder: Decoder: Schema
    ](
        wrapper: FormVariables => T,
        examples: Seq[InOutExample[?]] =
          inOutApi.apiExamples.inputExamples.fetchExamples
    ): Option[EndpointInput[T]] =
      inOutApi.inOut.in match
        case _: NoInput =>
          None
        case _ =>
          Some(
            jsonBody[T]
              .examples(examples.map { case ex @ InOutExample(label, _) =>
                Example(
                  wrapper(ex.toCamunda),
                  Some(label),
                  None
                )
              }.toList)
          )
  end extension

end CamundaPostmanApiCreator
