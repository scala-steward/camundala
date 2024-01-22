package camundala
package worker

import camundala.bpmn.*
import camundala.domain.*
import camundala.worker.CamundalaWorkerError.*

trait WorkerDsl:

  protected def engineContext: EngineContext

  protected def logger: WorkerLogger

  // needed that it can be called from CSubscriptionPostProcessor
  def worker: Worker[?, ?, ?, ?]

  def topic: String = worker.topic

  extension [T](option: Option[T])
    def toEither[E <: CamundalaWorkerError](
        error: E
    ): Either[E, T] =
      option
        .map(Right(_))
        .getOrElse(
          Left(error)
        )
  end extension // Option

end WorkerDsl

trait InitWorkerDsl[
    In <: Product: InOutCodec,
    Out <: Product: InOutCodec,
    InConfig <: Product: InOutCodec
] extends WorkerDsl,
      ValidateDsl[In],
      InitProcessDsl[In, InConfig]:

  protected def inOutExample: Process[In, Out, InConfig]

  protected lazy val defaultConfig: InConfig = inOutExample.inConfig
    
  lazy val worker: InitWorker[In, Out, InConfig] = 
    InitWorker(inOutExample)
    .validate(ValidationHandler(validate))
    .initProcess(InitProcessHandler(initProcess))


end InitWorkerDsl

trait CustomWorkerDsl[
    In <: Product: InOutCodec,
    Out <: Product: InOutCodec
] extends WorkerDsl,
      ValidateDsl[In],
      RunWorkDsl[In, Out]:

  protected def customTask: CustomTask[In, Out]

  lazy val worker: CustomWorker[In, Out] =
    CustomWorker(customTask)
      .validate(ValidationHandler(validate))
      .runWork(CustomHandler(runWork))

end CustomWorkerDsl

trait ServiceWorkerDsl[
    In <: Product: InOutCodec,
    Out <: Product: InOutCodec,
    ServiceIn <: Product: InOutEncoder,
    ServiceOut: InOutDecoder
] extends WorkerDsl,
      ValidateDsl[In],
      RunWorkDsl[In, Out]:

  lazy val worker: ServiceWorker[In, Out, ServiceIn, ServiceOut] =
    ServiceWorker[In, Out, ServiceIn, ServiceOut](serviceTask)
      .validate(ValidationHandler(validate))
      .runWork(
        ServiceHandler(
          method,
          apiUri,
          querySegments,
          inputMapper,
          inputHeaders,
          outputMapper,
          serviceTask.defaultServiceOutMock,
          serviceTask.serviceInExample
        )
      )

  // required
  protected def serviceTask: ServiceTask[In, Out, ServiceIn, ServiceOut]
  protected def apiUri(in: In): Uri // input must be valid - so no errors
  // optional
  protected def method: Method = Method.GET
  protected def querySegments(in: In): Seq[QuerySegmentOrParam] =
    Seq.empty // input must be valid - so no errors
  // mocking out from outService and headers
  protected def inputMapper(in: In): Option[ServiceIn] = None // input must be valid - so no errors
  protected def inputHeaders(in: In): Map[String, String] =
    Map.empty // input must be valid - so no errors
  protected def outputMapper(
      serviceOut: ServiceResponse[ServiceOut],
      in: In
  ): Either[ServiceMappingError, Out] = defaultOutMapper(serviceOut, in)

  /** Run the Work is done by the handler. If you want a different behavior, you need to use the
    * CustomWorkerDsl
    */
  final def runWork(
      inputObject: In
  ): Either[CustomError, Out] = Right(serviceTask.out)

  private def defaultOutMapper(
      serviceResponse: ServiceResponse[ServiceOut],
      in: In
  ): Either[ServiceMappingError, Out] =
    serviceResponse.outputBody match
      case _: NoOutput => Right(serviceTask.out)
      case _ =>
        Left(ServiceMappingError(s"There is an outputMapper missing for '${getClass.getName}'."))
  end defaultOutMapper

  protected def queryKeys(ks: String*): Seq[QuerySegmentOrParam] =
    ks.map(QuerySegmentOrParam.Key(_))

  protected def queryKeyValues(kvs: (String, Any)*): Seq[QuerySegmentOrParam] =
    kvs.map { case k -> v => QuerySegmentOrParam.KeyValue(k, s"$v") }

  protected def queryValues(vs: Any*): Seq[QuerySegmentOrParam] =
    vs.map(v => QuerySegmentOrParam.Value(s"$v"))

end ServiceWorkerDsl

private trait ValidateDsl[
    In <: Product: InOutCodec
]:

  def validate(in: In): Either[ValidatorError, In] = Right(in)

end ValidateDsl

private trait InitProcessDsl[
    In <: Product: InOutCodec,
    InConfig <: Product: InOutCodec
]:
  protected def defaultConfig: InConfig
  protected def engineContext: EngineContext

  def initProcess(in: In, optInConfig: Option[InConfig]): Either[InitProcessError, Map[String, Any]] =
    Right(optInConfig.map(initConfig).getOrElse(Map.empty))

  /** initialize the config of the form of:
    * ```
    * case class InConfig(
    * timerIdentificationNotReceived: Option[String :| Iso8601Duration],
    * timerEBankingContractCheckOpened: Option[String :| CronExpr] =
    * ...
    * )
    * ```
    */
  private def initConfig[T <: Product: InOutEncoder](
      config: T, 
  ): Map[String, Any] =
    val json = config.asJson
    val defaultJson = defaultConfig.asJson
    val r =
      config.productElementNames
        .map(k =>
          k -> (json.hcursor
            .downField(k).focus, defaultJson.hcursor 
            .downField(k).focus)
        ).collect {
          case k -> (Some(j), Some(dj)) if j.isNull =>
            k -> dj
          case k -> (Some(j), _) =>
            k -> j
          case k -> (_, dj) =>
            k -> dj.getOrElse(Json.Null)
        }
        .toMap
    end r
    engineContext.toEngineObject(r)
  end initConfig

end InitProcessDsl

private trait RunWorkDsl[
    In <: Product: InOutCodec,
    Out <: Product: InOutCodec
]:

  def runWork(
      inputObject: In
  ): Either[CustomError, Out]

end RunWorkDsl
