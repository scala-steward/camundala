package pme123.camundala.examples.playground

import org.springframework.boot.autoconfigure.SpringBootApplication
import pme123.camundala.app.appRunner.AppRunner
import pme123.camundala.examples.common.StandardCliApp
import pme123.camundala.services.StandardApp
import pme123.camundala.services.StandardApp.StandardAppDeps
import zio.ZLayer

@SpringBootApplication
class PlaygroundApp

object PlaygroundApp extends StandardCliApp {

  protected val ident: String = "playground"
  protected val title = "Camundala Playground App"

  protected def appRunnerLayer: ZLayer[StandardAppDeps, Nothing, AppRunner] =
    StandardApp.layer(classOf[PlaygroundApp], "playgroundModels.sc")

}
