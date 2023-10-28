package camundala
package worker

import camundala.domain.*
import camundala.worker.CamundalaWorkerError.*

case class WorkerExecutor[
    In <: Product: CirceCodec,
    Out <: Product: CirceCodec,
    T <: Worker[In, Out, ?]
](
    worker: T
)(using context: EngineContext):

  def execute(
      processVariables: Seq[Either[BadVariableError, (String, Option[Json])]],
  ) =
    for {
      validatedInput <- InputValidator.validate(processVariables)
      initializedOutput <- Initializer.initVariables(validatedInput)
      proceedOrMocked <- OutMocker.mockOrProceed(
        context.generalVariables
      )
      output <- worker.workRunner
        .map(_.runWork(validatedInput, proceedOrMocked))
        .getOrElse(Right(None))
      allOutputs = camundaOutputs(
        validatedInput,
        initializedOutput,
        output
      )
      filteredOut = filteredOutput(allOutputs)
    } yield filteredOut

  object InputValidator:
    lazy val prototype = worker.in
    lazy val validationHandler = worker.validationHandler

    def validate(
        inputParamsAsJson: Seq[Either[Any, (String, Option[Json])]]
    ): Either[ValidatorError, In] =
      val jsonResult: Either[ValidatorError, Seq[(String, Option[Json])]] =
        inputParamsAsJson
          .partition(_.isRight) match
          case (successes, failures) if failures.isEmpty =>
            Right(
              successes.collect { case Right(value) => value }
            )
          case (_, failures) =>
            Left(
              ValidatorError(
                failures
                  .collect { case Left(value) => value }
                  .mkString("Validator Error(s):\n - ", " - ", "\n")
              )
            )
      val json: Either[ValidatorError, JsonObject] = jsonResult
        .map(_.foldLeft(JsonObject()) { case (jsonObj, jsonKey -> jsonValue) =>
          println(
            s" - $jsonKey: ${jsonValue.getClass.getSimpleName} - $jsonValue"
          )
          jsonObj.add(jsonKey, jsonValue.getOrElse(Json.Null))
        })
      json
        .flatMap(jsonObj =>
          decodeTo[In](jsonObj.asJson.toString).left
            .map(ex => ValidatorError(errorMsg = ex.errorMsg))
            .flatMap(in => validationHandler.map(h => h.validate(in)).getOrElse(Right(in)))
        )
    end validate

  end InputValidator

  object Initializer:
    private val defaultVariables = Map(
      "serviceName" -> "NOT-USED" // serviceName is not needed anymore
    )

    def initVariables(
        validatedInput: In
    ): Either[InitializerError, Map[String, Any]] = worker.variablesInit
      .map { vi =>
        vi(validatedInput).map(_ ++ defaultVariables)
      }
      .getOrElse(Right(defaultVariables))
  end Initializer

  object OutMocker:

    def mockOrProceed(
        generalVariables: GeneralVariables
    ): Either[MockerError | MockedOutput, Option[Out]] =
      ((
        generalVariables.servicesMocked,
        generalVariables.isMocked(worker.topic),
        generalVariables.outputMockOpt
      ) match
        case (
              _,
              _,
              Some(outputMock)
            ) => // if the outputMock is set than we mock
          decodeMock(isService, outputMock)
        case (_, true, _)
            if !isService => // if your process is NOT a Service check if it is mocked
          worker.defaultMock
        case (true, _, _)
            if isService => // if your process is a Service check if it is mocked
          worker.defaultMock
        case (_, _, None) =>
          Right(None)
      )

    end mockOrProceed

    private lazy val isService = worker.isInstanceOf[ServiceWorker[?, ?, ?, ?]]

  end OutMocker

  object WorkRunner:
    def run() =
      worker.workRunner
  end WorkRunner

  private def camundaOutputs(
      initializedInput: In,
      internalVariables: Map[String, Any],
      output: Option[Out]
  ): Map[String, Any] =
    context.toEngineObject(initializedInput) ++ internalVariables ++ output
      .map(context.toEngineObject)
      .getOrElse(Map.empty)

  private def filteredOutput(
      allOutputs: Map[String, Any]
  ): Map[String, Any] =
    val filter = context.generalVariables.outputVariables
    if (filter.isEmpty)
      allOutputs
    else
      allOutputs
        .filter { case k -> _ => filter.contains(k) }

end WorkerExecutor