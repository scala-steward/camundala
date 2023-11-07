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
    ServiceIn <: Product: CirceCodec,
    ServiceOut: CirceCodec
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
          defaultHeaders,
          inputMapper,
          outputMapper
        )
      )
    
  // required  
  protected def serviceTask: ServiceTask[In, Out, ServiceIn, ServiceOut]
  protected def apiUri: Uri
  // optional
  protected def method: Method = Method.GET
  protected def queryParamKeys: Seq[String | (String, String)] = Seq.empty
  // mocking out from outService and headers
  protected def defaultHeaders: Map[String, String] = Map.empty
  protected def inputMapper(in: In): Option[ServiceIn] = None
  protected def outputMapper(
      out: ServiceResponse[ServiceOut]
  ): Either[ServiceMappingError, Option[Out]] = Right(None)

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
      inputObject: In,
      optOutput: Option[Out]
  ): Either[CustomError, Option[Out]] = Right(optOutput)

end RunWorkDsl
