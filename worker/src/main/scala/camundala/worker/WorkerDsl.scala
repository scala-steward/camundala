package camundala
package worker

import camundala.bpmn.*
import camundala.domain.*
import camundala.worker.CamundalaWorkerError.*

import scala.language.implicitConversions

trait WorkerDsl:

  protected def engineContext: EngineContext

  protected def logger: WorkerLogger

  // needed that it can be called from CSubscriptionPostProcessor
  def worker: Worker[?, ?, ?]

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

trait InitProcessWorkerDsl[
    In <: Product: CirceCodec,
    Out <: Product: CirceCodec
] extends WorkerDsl,
      ValidateDsl[In],
      InitProcessDsl[In]:


  lazy val worker: InitProcessWorker[In, Out] = InitProcessWorker(process)
    .validate(ValidationHandler(validate))
    .initProcess(InitProcessHandler(initProcess))

  protected def process: Process[In, Out]

end InitProcessWorkerDsl

trait CustomWorkerDsl[
    In <: Product: CirceCodec,
    Out <: Product: CirceCodec
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
    In <: Product: CirceCodec,
    Out <: Product: CirceCodec,
    ServiceIn <: Product: Encoder,
    ServiceOut: Decoder
] extends WorkerDsl,
      ValidateDsl[In],
      RunWorkDsl[In, Out]:

  lazy val worker: ServiceWorker[In, Out, ServiceIn, ServiceOut] =
    ServiceWorker(serviceTask)
      .validate(ValidationHandler(validate))
      .runWork(
        ServiceHandler(
          method,
          apiUri,
          queryParamKeys,
          inputMapper,
          inputHeaders,
          outputMapper,
          serviceTask.defaultServiceOutMock
        )
      )

  // required
  protected def serviceTask: ServiceTask[In, Out, ServiceIn, ServiceOut]
  protected def apiUri(in: In): Uri
  // optional
  protected def method: Method = Method.GET
  protected def queryParamKeys: Seq[String | (String, String)] = Seq.empty
  // mocking out from outService and headers
  protected def inputMapper(in: In): Option[ServiceIn] = None
  protected def inputHeaders(in: In):Map[String, String] = Map.empty
  protected def outputMapper(
                              serviceOut: ServiceResponse[ServiceOut]
  ): Either[ServiceMappingError, Out] = defaultOutMapper(serviceOut)

  /** Run the Work is done by the handler. If you want a different behavior, you need to use the
    * CustomWorkerDsl
    */
  final def runWork(
      inputObject: In
  ): Either[CustomError, Out] = Right(serviceTask.out)

  private def defaultOutMapper(serviceResponse: ServiceResponse[ServiceOut]): Either[ServiceMappingError, Out] =
    serviceResponse.outputBody match
      case _: NoOutput => Right(serviceTask.out)
      case _ =>
        Left(ServiceMappingError(s"There is an outputMapper missing for '${getClass.getName}'."))
  end defaultOutMapper
end ServiceWorkerDsl

private trait ValidateDsl[
    In <: Product: CirceCodec
]:

  def validate(in: In): Either[ValidatorError, In] = Right(in)

end ValidateDsl

private trait InitProcessDsl[
    In <: Product: CirceCodec
]:

  def initProcess(in: In): Either[InitProcessError, Map[String, Any]] = Right(Map.empty)

end InitProcessDsl

private trait RunWorkDsl[
    In <: Product: CirceCodec,
    Out <: Product: CirceCodec
]:

  def runWork(
      inputObject: In
  ): Either[CustomError, Out]

end RunWorkDsl
