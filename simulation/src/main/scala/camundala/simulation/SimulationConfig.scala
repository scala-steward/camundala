package camundala.simulation

/**
 *
 * @tparam B Builder for Authentication and preRequest
 */
case class SimulationConfig[B](
                             // define tenant if you have one
                             tenantId: Option[String] = None,
                             // the Camunda Port
                             // there are Requests that wait until the process is ready - like getTask.
                             // the Simulation waits 1 second between the Requests.
                             // so with a timeout of 10 sec it will try 10 times (retryDuration = 1.second)
                             maxCount: Int = 10,
                             // the number of parallel execution of a simulation.
                             // for example run the process 3 times (userAtOnce = 3)
                             userAtOnce: Int = 1,
                             // you can run the requess of a scenario multiple times. (the preRequests will be run only once - e.g. get a token)
                             executionCount: Int = 1,
                             // add requests that needed to be executed before the test requests.
                             // example get the token for OAuth2
                             // they must be lazy - otherwise they are blocking!?
                             preRequests: Seq[() => B] = Nil,
                             // REST endpoint of Camunda
                             endpoint: String = "http://localhost:8080/engine-rest",
                             // you can add authentication with this - default there is none.
                             // see BasicSimulationDsl / OAuthSimulationDsl for examples
                             authHeader: B => B = (b: B) => b
                           ):

  def withTenantId(tenantId: String): SimulationConfig[B] =
    copy(tenantId = Some(tenantId))

  def withMaxCount(maxCount: Int): SimulationConfig[B] =
    copy(maxCount = maxCount)

  def withTenantId(userAtOnce: Int = 1): SimulationConfig[B] =
    copy(userAtOnce = userAtOnce)


  def withPreRequest(preRequest: () => B): SimulationConfig[B] =
    copy(preRequests = preRequests :+ preRequest)

  def withAuthHeader(authHeader: B => B = b => b): SimulationConfig[B] =
    copy(authHeader = authHeader)

  def withPort(port: Int): SimulationConfig[B] =
    copy(endpoint = s"http://localhost:$port/engine-rest")

  def withUserAtOnce(userAtOnce: Int): SimulationConfig[B] =
    copy(userAtOnce = userAtOnce)

  def withExecutionCount(executionCount: Int): SimulationConfig[B] =
    copy(executionCount = executionCount)
