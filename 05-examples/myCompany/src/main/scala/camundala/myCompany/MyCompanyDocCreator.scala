package camundala.myCompany

import camundala.api.*
import camundala.helper.dev.company.docs.DocCreator

/*
Starting point to use Camundala for Company wide documentation
For now only the catalog is created.
exampleMyCompany/run
 */
object MyCompanyDocCreator extends DocCreator, App:
  given apiConfig: ApiConfig = myCompanyConfig
  protected def upload(releaseTag: String): Unit =
    println("Uploaded to Web Server")

  prepareDocs()
end MyCompanyDocCreator
