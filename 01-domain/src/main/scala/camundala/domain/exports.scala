package camundala
package domain

import io.circe.derivation.Configuration
import io.circe.parser
import sttp.model.Uri
import sttp.tapir.Schema

import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.Base64
import scala.deriving.Mirror
import scala.language.implicitConversions

// circe
export io.circe.{Codec as CirceCodec}
export sttp.tapir.Schema.annotations.description

implicit val c: Configuration = Configuration.default.withDefaults
  .withDiscriminator("type")
type ConfiguredEnumCodec[T] = io.circe.derivation.ConfiguredEnumCodec[T]
type ConfiguredCodec[T] = io.circe.derivation.ConfiguredCodec[T]

type JsonCodec[T] = io.circe.Codec[T]

inline def deriveCodec[A](using inline A: Mirror.Of[A]): JsonCodec[A] =
  io.circe.derivation.ConfiguredCodec.derived(using
  Configuration.default // .withDefaults
    .withDiscriminator("type"))
inline def deriveEnumCodec[A](using inline A: Mirror.SumOf[A]): JsonCodec[A] =
  io.circe.derivation.ConfiguredEnumCodec.derived(using
    Configuration.default // .withDefaults
      .withoutDiscriminator
    // .withDiscriminator("type")
  )

type Json = io.circe.Json
type JsonEncoder[T] = io.circe.Encoder[T]
inline def deriveEncoder[A](using inline A: Mirror.Of[A]): JsonEncoder[A] =
  io.circe.generic.semiauto.deriveEncoder
type JsonDecoder[T] = io.circe.Decoder[T]
inline def deriveDecoder[A](using inline A: Mirror.Of[A]): JsonDecoder[A] =
  io.circe.generic.semiauto.deriveDecoder

type ApiSchema[T] = Schema[T]
inline def deriveSchema[T](using
    m: Mirror.Of[T]
): Schema[T] =
  Schema.derived[T]
inline def deriveEnumSchema[T](using
    m: Mirror.Of[T]
): Schema[T] =
  Schema.derivedEnumeration[T].defaultStringBased

// Circe Enum codec
// used implicit instead of given - so no extra import is needed domain.{*, given}

case class NoInput()
object NoInput:
  given ApiSchema[NoInput] = deriveSchema
  given JsonCodec[NoInput] = deriveCodec

case class NoOutput()
object NoOutput:
  given ApiSchema[NoOutput] = deriveSchema
  given JsonCodec[NoOutput] = deriveCodec

enum NotValidStatus:
  case notValid
object NotValidStatus:
  given ApiSchema[NotValidStatus] = deriveEnumSchema
  given JsonCodec[NotValidStatus] = deriveEnumCodec

enum CanceledStatus:
  case canceled
object CanceledStatus:
  given ApiSchema[CanceledStatus] = deriveEnumSchema
  given JsonCodec[CanceledStatus] = deriveEnumCodec

@deprecated
trait GenericServiceIn:
  def serviceName: String

case class FileInOut(
    fileName: String,
    @description("The content of the File as a Byte Array.")
    content: Array[Byte],
    mimeType: Option[String]
):
  lazy val contentAsBase64: String = Base64.getEncoder.encodeToString(content)
end FileInOut

object FileInOut:
  given ApiSchema[FileInOut] = deriveSchema
  given JsonCodec[FileInOut] = deriveCodec

/** In Camunda 8 only json is allowed!
  */
case class FileRefInOut(
    fileName: String,
    @description("A reference to retrieve the file in your application.")
    ref: String,
    mimeType: Option[String]
)

object FileRefInOut:
  given ApiSchema[FileRefInOut] = deriveSchema
  given JsonCodec[FileRefInOut] = deriveCodec

// Use this in the DSL to avoid Option[?]
// see https://stackoverflow.com/a/69925310/2750966
case class Optable[Out](value: Option[Out])

object Optable:
  given fromOpt[T]: Conversion[Option[T], Optable[T]] = Optable(_)
  given fromValue[T]: Conversion[T, Optable[T]] = v => Optable(Option(v))

//json
def throwErr(err: String) =
  throw new IllegalArgumentException(err)

def toJson(json: String): Json =
  parser.parse(json) match
    case Right(v) => v.deepDropNullValues
    case Left(exc) =>
      throwErr(s"Could not create Json from your String -> $exc")
val testModeDescr =
  "This flag indicades that this is a test - in the process it can behave accordingly."

// descriptions
val deprecatedDescr =
  "See https://pme123.github.io/camundala/specification.html#supported-general-variables"
@deprecated("Change to serviceTask")
def serviceNameDescr(serviceName: String) =
  s"As this uses the generic Service you need to name the Service to '$serviceName'."

@deprecated(
  "If you mock another Service or Subprocess - use `serviceOrProcessMockDescr` - otherwise:\n\n" + deprecatedDescr
)
def outputMockDescr[Out: JsonEncoder](mock: Out) =
  serviceOrProcessMockDescr(mock)

def serviceOrProcessMockDescr[Out: JsonEncoder](mock: Out) =
  s"""You can mock the response variables of this (sub)process.
     |
     |Class: `${mock.getClass.getName.replace("$", " > ")}`
     |
     |Here an example:
     |
     |```scala
     |${mock.asJson}
     |```
     |
     |General to mocking:
     |
     |- `outputMock` mocks this process.
     |- `someSubProcessMock` mocks a sub process
     |""".stripMargin

@deprecated(deprecatedDescr)
val defaultMockedDescr =
  "This flag will mock every Service that this Process calls, using the default Mock."

@deprecated(deprecatedDescr)
def outputServiceMockDescr[ServiceOut: JsonEncoder](mock: ServiceOut) =
  s"""You can mock the response variables of this Http Service.
     |
     |Class: `${mock.getClass.getName.replace("$", " > ")}`
     |
     |Here an example:
     |
     |```scala
     |MockedHttpResponse.success200(
     |  ${mock.asJson}
     |)
     |```
     |
     |General to mocking:
     |
     |- `outputMock` mocks this process.
     |- `someSubProcessMock` mocks a sub process
     |""".stripMargin

@deprecated(deprecatedDescr)
val handledErrorsDescr =
  "A comma separated list of HTTP-Status-Codes, that are modelled in the BPMN as Business-Exceptions - see Outputs. z.B: `404,500`"
val regexHandledErrorsDescr =
  """If you specified _handledErrors_, you can specify Regexes that all must match the error messages.
Otherwise the error is thrown.
  
You can use a JSON Array of Strings or a comma-separated String.  

Example: `['java.sql.SQLException', '"errorNr":20000']` or 'java.sql.SQLException,"errorNr":20000'
"""

def prettyUriString(uri: Uri) =
  URLDecoder.decode(
    uri.toString,
    Charset.defaultCharset()
  )
def prettyString(obj: Any, depth: Int = 0, paramName: Option[String] = None): String =
  val indent = "  " * depth
  val prettyName = paramName.fold("")(x => s"$x: ")
  val ptype = obj match
    case _: Iterable[Any] => ""
    case obj: Product => obj.productPrefix
    case _ => obj.toString
  val nameWithType = s"\n$indent$prettyName$ptype"

  obj match
    case None => ""
    case Some(value) => s"${prettyString(value, depth, paramName)}"
    case uri: Uri => s"\n$indent$prettyName${prettyUriString(uri)}"
    case seq: Iterable[Any] =>
      val seqStr = seq.map(prettyString(_, depth + 1))
      if seqStr.isEmpty then "" else s"$nameWithType[${seqStr.mkString}\n$indent]"
    case obj: Product =>
      val objStr = (obj.productIterator zip obj.productElementNames)
        .map { case (subObj, paramName) => prettyString(subObj, depth + 1, Some(paramName)) }
      if objStr.isEmpty then s"$nameWithType" else s"$nameWithType{${objStr.mkString}\n$indent}"
    case _ =>
      s"$nameWithType"
  end match
end prettyString
