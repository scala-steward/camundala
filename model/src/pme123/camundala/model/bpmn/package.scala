package pme123.camundala.model

import eu.timepit.refined.{W, refineV}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import zio.ZIO

package object bpmn {
  private type OrgNameRegex = MatchesRegex[W.`"""(?:^\\w+|\\w+\\.\\w+)+$"""`.T]

  type IdRegex = MatchesRegex[W.`"""(?:^\\w+|\\w+(-|\\.)\\w+)+$"""`.T]
  type FileNameRegex = MatchesRegex[W.`"""(?:^\\w+|\\w+(-|/|\\.)\\w+)+$"""`.T]

  type BpmnId = String Refined IdRegex
  type FileName = String Refined FileNameRegex

  type OrgName = String Refined OrgNameRegex

  def bpmnIdFromFileName(fileName: FileName): ZIO[Any, ModelException, BpmnId] =
    bpmnIdFromStr(fileName.value)

  def bpmnIdFromStr(str: String): ZIO[Any, ModelException, BpmnId] =
    ZIO.fromEither(refineV[IdRegex](str.split("/").last))
      .mapError(ex => ModelException(s"Could not map FileName $str to BpmnId:\n $ex"))

  def fileNameFromBpmnId(bpmnId: BpmnId): ZIO[Any, ModelException, FileName] =
    fileNameFromStr(bpmnId.value)

  def fileNameFromStr(fileName: String): ZIO[Any, ModelException, FileName] =
    ZIO.fromEither(refineV[FileNameRegex](fileName.split("/").last))
      .mapError(ex => ModelException(s"Could not create FileName $fileName:\n $ex"))
}
