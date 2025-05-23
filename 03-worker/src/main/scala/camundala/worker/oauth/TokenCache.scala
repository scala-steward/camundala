package camundala.worker.oauth

import com.github.blemale.scaffeine.{Cache, Scaffeine}

import scala.concurrent.duration.*

object TokenCache:

  lazy val cache: Cache[String, String] =
    Scaffeine()
      .recordStats()
      .expireAfterWrite(4.minutes)
      .maximumSize(1000)
      .build[String, String]()
end TokenCache
