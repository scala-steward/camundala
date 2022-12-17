package camundala
package simulation

import camundala.api.CamundaProperty
import camundala.bpmn.*
import camundala.bpmn.CamundaVariable.*
import camundala.domain.*
import camundala.simulation.TestOverrideType.*
import io.circe.parser.*

trait ResultChecker {

  def checkProps(
      withOverrides: WithTestOverrides[_],
      result: Seq[CamundaProperty]
  ): Boolean =
    withOverrides.testOverrides match
      case Some(TestOverrides(overrides)) =>
        checkO(overrides, result)
      case _ =>
        checkP(withOverrides.camundaToCheckMap, result)

  private def checkO(
      overrides: Seq[TestOverride],
      result: Seq[CamundaProperty]
  ) =
    overrides
      .map {
        case TestOverride(Some(k), Exists, _) =>
          val matches = result.exists(_.key == k)
          if (!matches)
            println(s"!!! $k did NOT exist in $result")
          matches
        case TestOverride(Some(k), NotExists, _) =>
          val matches = !result.exists(_.key == k)
          if (!matches)
            println(s"!!! $k did EXIST in $result")
          matches
        case TestOverride(Some(k), IsEquals, Some(v)) =>
          val r = result.find(_.key == k)
          val matches = r.nonEmpty && r.exists(_.value == v)
          if (!matches)
            println(s"!!! $v ($k) is NOT equal in $r")
          matches
        case TestOverride(Some(k), HasSize, Some(value)) =>
          val r = result.find(_.key == k)
          val matches = r.exists {
            _.value match
              case CJson(j, _) =>
                (toJson(j).asArray, value) match
                  case (Some(vector), CInteger(s, _)) =>
                    vector.size == s
                  case _ =>
                    false
              case _ => false
          }
          if (!matches)
            println(s"!!! $k has NOT Size $value in $r")
          matches
        case _ =>
          println(
            s"!!! Only ${TestOverrideType.values.mkString(", ")} for TestOverrides supported."
          )
          false
      }
      .forall(_ == true)

  def checkOForCollection(
      overrides: Seq[TestOverride],
      result: Seq[CamundaVariable | Map[String, CamundaVariable]]
  ) =
    overrides
      .map {
        case TestOverride(None, HasSize, Some(CInteger(size, _))) =>
          val matches = result.size == size
          if (!matches)
            println(
              s"!!! Size '${result.size}' of collection is NOT equal to $size in $result"
            )
          matches
        case TestOverride(None, Contains, Some(expected)) =>
          val exp = expected match
            case CJson(jsonStr, _) =>
              parse(jsonStr) match
                case Right(json) =>
                  CamundaVariable.jsonToCamundaValue(json)
                case Left(ex) =>
                  throwErr(s"Problem parsing Json: $jsonStr\n$ex")
            case other => other
          val matches = result.contains(exp)
          if (!matches)
            println(
              s"!!! Result '$result' of collection does NOT contain to $expected"
            )
          matches
        case _ =>
          println(
            s"!!! Only ${TestOverrideType.values.mkString(", ")} for TestOverrides supported."
          )
          false
      }
      .forall(_ == true)

  private def checkP[T <: Product](
      camundaVariableMap: Map[String, CamundaVariable],
      result: Seq[CamundaProperty]
  ): Boolean =
    camundaVariableMap
      .map { case key -> expectedValue =>
        result
          .find(_.key == key)
          .map {
            case CamundaProperty(
                  _,
                  cValue @ CFile(
                    _,
                    cFileValueInfo @ CFileValueInfo(cFileName, _),
                    _
                  )
                ) =>
              val matches = expectedValue match
                case CFile(_, CFileValueInfo(pFileName, _), _) =>
                  cFileName == pFileName
                case o =>
                  false
              if (!matches)
                println(
                  s"<<< cFile: ${cValue.getClass} / expectedFile: ${expectedValue.getClass}"
                )
                println(
                  s"!!! The expected File value '${expectedValue}'\n of $key does not match the result variable: '$cFileValueInfo'."
                )
              matches
            case CamundaProperty(_, CJson(cValue, _)) =>
              val cJson = toJson(cValue)
              val pJson = toJson(expectedValue.value.toString)
              val setCJson = cJson.as[Set[Json]].toOption.getOrElse(cJson)
              val setPJson = pJson.as[Set[Json]].toOption.getOrElse(pJson)
              val matches: Boolean = setCJson == setPJson
              if (!matches)
                println(
                  s"<<< cJson: ${setCJson.getClass} / expectedJson: ${setPJson.getClass}"
                )
                println(
                  s"!!! The expected Json value '$setPJson' of $key does not match the result variable cJson: '$setCJson'."
                )
              matches
            case CamundaProperty(_, cValue) =>
              val matches: Boolean = cValue.value == expectedValue.value
              if (!matches)
                println(
                  s"<<< cValue: ${cValue.getClass} / expectedValue ${expectedValue.getClass}"
                )
                println(
                  s"!!! The expected value '$expectedValue' of $key does not match the result variable '${cValue}'.\n $result"
                )
              matches
          }
          .getOrElse {
            println(
              s"!!! $key does not exist in the result variables.\n $result"
            )
            false
          }
      }
      .forall(_ == true)
}