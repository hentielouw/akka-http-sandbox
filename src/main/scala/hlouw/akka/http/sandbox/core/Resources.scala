package hlouw.akka.http.sandbox.core

import akka.http.scaladsl.server.Directives._
import hlouw.akka.http.sandbox.resources.RabbitResource

trait Resources {
  this: Services with Core =>

  lazy val rabbitResource = new RabbitResource(rabbitService)

  lazy val routes = logRequestResult("akka-http-sandbox") {
    rabbitResource.routes
  }
}
