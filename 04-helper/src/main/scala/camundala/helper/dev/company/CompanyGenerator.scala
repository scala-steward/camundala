package camundala.helper.dev.company

import camundala.helper.dev.update.*

case class CompanyGenerator()(using config: DevConfig):

  lazy val generate: Unit =
    generateDirectories
    DirectoryGenerator().generate // generates myCompany-camundala project
    GenericFileGenerator().createScalaFmt
    GenericFileGenerator().createGitIgnore
    // needed helper classes
    CompanyWrapperGenerator().generate
    // override helperCompany.scala
    createOrUpdate(os.pwd / "helperCompany.scala", CompanyScriptCreator().companyHelper)
    // sbt
    CompanySbtGenerator().generate
    // company-camundala
    // helper.scala
    createOrUpdate(companyCamundala / "helper.scala", CompanyScriptCreator().companyCamundalaHelper)
    // docs
    CompanyDocsGenerator(companyCamundala).generate
  end generate

  lazy val createProject: Unit =
    generateProjectDirectories
    createOrUpdate(projects / projectName / "helper.scala", ScriptCreator().projectHelper)
  end createProject

  private lazy val companyName = config.companyName
  private lazy val projectName = config.projectName

  private lazy val generateDirectories: Unit =
    os.makeDir.all(gitTemp)
    os.makeDir.all(docker)
    os.makeDir.all(companyCamundala)
    os.makeDir.all(projects)

  end generateDirectories

  private lazy val generateProjectDirectories: Unit =
    os.makeDir.all(projects / projectName)

  private lazy val gitTemp = os.pwd / "git-temp"
  private lazy val docker = os.pwd / "docker"
  private lazy val companyCamundala = os.pwd / s"$companyName-camundala"
  private lazy val projects = os.pwd / "projects"
end CompanyGenerator

object CompanyGenerator:
  def init(companyName: String) = //, repoConfig: RepoConfig.Artifactory) =
    DevConfig.defaultConfig(s"$companyName-camundala")
  end init

end CompanyGenerator
