package camundala.examples.twitter.c7

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
@EnableProcessApplication
class TwitterServletProcessApplication

object TwitterServletProcessApplication:

  def main(args: Array[String]): Unit =
    SpringApplication.run(classOf[TwitterServletProcessApplication], args*)
end TwitterServletProcessApplication
