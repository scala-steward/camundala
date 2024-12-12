package camundala.helper.dev.company

import camundala.helper.dev.update.doNotAdjust
import camundala.helper.util.{DevConfig, VersionHelper}

case class CompanyScriptCreator()(using config: DevConfig):

  lazy val companyHelper =
    s"""#!/usr/bin/env -S scala shebang
       |// $doNotAdjust. This file is replaced by `./helperCompany.scala init`.
       |
       |//> using toolkit 0.5.0
       |//> using dep io.github.pme123::camundala-helper:${VersionHelper.camundalaVersion}
       |
       |import camundala.helper.dev.DevCompanyHelper
       |
       |   @main
       |   def run(command: String, arguments: String*): Unit =
       |     DevCompanyHelper.run(command, arguments*)
       |""".stripMargin


end CompanyScriptCreator