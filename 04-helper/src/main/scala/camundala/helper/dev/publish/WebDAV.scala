package camundala.helper.dev.publish

import camundala.api.ApiConfig
import camundala.helper.util.{Helpers, PublishConfig}
import com.github.sardine.SardineFactory
import com.github.sardine.impl.SardineException

import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.CollectionHasAsScala

abstract class WebDAV:
  def publishConfig: PublishConfig
  val baseUrl = publishConfig.documentationUrl
  val contentTypeHtml = "text/html"
  val contentTypeYaml = "text/yaml"

  protected def startSession =
    val sardine = SardineFactory.begin

    // set Credentials
    (for
      username <- sys.env.get(publishConfig.documentationEnvUsername)
      password <- sys.env.get(publishConfig.documentationEnvPassword)
    yield
        println(s"Set Credentials for $username (${publishConfig.documentationEnvUsername})")
        sardine.setCredentials(username, password))
      .getOrElse(
        throw new IllegalArgumentException(
          s"System Environment Variables ${publishConfig.documentationEnvUsername} and/ or ${publishConfig.documentationEnvPassword} are not set."
        )
      )
    sardine
  end startSession
end WebDAV

case class ProjectWebDAV(projectName: String, publishConfig: PublishConfig) extends WebDAV:

  val projectUrl = s"$baseUrl/$projectName/"

  def upload(): Unit =
    println(s"Start $projectName: upload Documentation to ${publishConfig.documentationUrl}")
    val sardine = startSession
    try
      // remove existing project if exists
      try
        val existingFiles = sardine.list(
          projectUrl
        ) // sardine.exists does not work (is not allowed)
        if !existingFiles.isEmpty then
          println("Delete existing")
          sardine.delete(projectUrl)
      catch
        case ex: SardineException
          if ex.getMessage.contains("Unexpected response (404 Not Found)") =>
          println(s"New Project will be created.")
      end try

      // create new
      sardine.createDirectory(projectUrl)
      sardine.put(s"$projectUrl/OpenApi.html", openApiHtml, contentTypeHtml)
      sardine.put(s"$projectUrl/openApi.yml", openApiYml, contentTypeYaml)
      postmanApiYml.foreach(pApi =>
        sardine.put(
          s"$projectUrl/postmanCollection.json",
          pApi,
          contentTypeYaml
        )
      )
      // additional files
      val docDir = os.pwd / "doc"
      if docDir.toIO.exists() then
        sardine.createDirectory(s"$projectUrl/doc/")
        val docFiles = os.list(docDir)
        docFiles.foreach { f =>
          println(s"Uploading $projectUrl/doc/${f.toIO.getName}")
          sardine.put(
            s"$projectUrl/doc/${f.toIO.getName}",
            os.read.inputStream(f)
          )
        }
      end if
      // diagrams
      val diagramDir = os.pwd / "src" / "main" / "resources" / "camunda"
      sardine.createDirectory(s"$projectUrl/diagrams/")
      val diagramFiles = os.list(diagramDir)
      diagramFiles
        .filter(p =>
          p.toString().endsWith(".bpmn") || p.toString().endsWith(".dmn")
        )
        .foreach { f =>
          println(s"Uploading $projectUrl/diagrams/${f.toIO.getName}")
          sardine.put(
            s"$projectUrl/diagrams/${f.toIO.getName}",
            os.read.inputStream(f)
          )
        }

      println(s"Finished $projectName: upload Documentation")
    finally sardine.shutdown()
    end try

  private lazy val openApiHtml =
    os.read.inputStream(publishConfig.openApiHtmlPath)
  private lazy val openApiYml = os
    .read(os.pwd / "openApi.yml")
    .getBytes(StandardCharsets.UTF_8)
  private lazy val postmanApiYml =
    val path = os.pwd / "postmanCollection.json"
    if path.toIO.exists() then
      Some(path.getInputStream)
    else None
  end postmanApiYml

end ProjectWebDAV

case class DocsWebDAV(apiConfig: ApiConfig, publishConfig: PublishConfig) extends WebDAV with Helpers:
  def upload(releaseTag: String): Unit =
    val sardine = startSession
    val directories = Seq("helium", "releases")
    val docDir = os.pwd / "target" / "docs" / "site"
    if docDir.toIO.exists() then
      // symbolic links for dependencies
      os.proc(
        "ln",
        "-s",
        s"./$releaseTag/dependencies",
        docDir / "dependencies"
      ).callOnConsole()
      os.proc("ln", "-s", s"./$releaseTag/helium", docDir / "helium")
        .callOnConsole()
      os.proc("ln", "-s", s"./$releaseTag/index.html", docDir / "index.html")
        .callOnConsole()
      os.proc(
        "ln",
        "-s",
        s"./$releaseTag/overviewDependencies.html",
        docDir / "overviewDependencies.html"
      ).callOnConsole()
      try
        // remove existing files if exists
        val existingFiles = sardine.list(baseUrl)
        val filteredFiles = existingFiles.asScala
          .filter(r =>
            r.getName.endsWith(".html") || directories.contains(r.getName)
          )
        println(s"Existing Files $existingFiles")
        println(s"Filtered Files $filteredFiles")
        filteredFiles
          .filterNot(_.getName == "releases")
          .foreach(f =>
            val url =
              if f.isDirectory then s"$baseUrl/${f.getName}/"
              else s"$baseUrl/${f.getName}"
            println(s"Delete: $url")
            sardine.delete(url)
          )

        def uploadFiles(url: String, docFiles: Seq[os.Path]): Unit =
          docFiles.foreach {
            case f if f.toIO.isDirectory && f.toIO.exists() =>
              println(s"Create Directory $url/${f.toIO.getName}")
              //  if (!sardine.exists(s"$url/${f.toIO.getName}/"))
              //    sardine.createDirectory(s"$url/${f.toIO.getName}/")
              uploadFiles(s"$url/${f.toIO.getName}", os.list(f))
            case f if f.toIO.exists() =>
              println(s"Uploading $url/${f.toIO.getName}")
              sardine.put(s"$url/${f.toIO.getName}", os.read.inputStream(f))
            case f =>
              println(s"Not supported file: $f")
          }

        uploadFiles(baseUrl, os.list(docDir))
        println(s"Finished upload Documentation")

      catch
        case ex: SardineException
            if ex.getMessage == "Unexpected response (404 Not Found)" =>
          println(s"New Project will be created.")
      finally
        sardine.shutdown()
      end try
    else
      throw new IllegalStateException(
        "This Task is only possible for company-docs project."
      )
    end if
  end upload
end DocsWebDAV