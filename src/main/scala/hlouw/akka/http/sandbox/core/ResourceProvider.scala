package hlouw.akka.http.sandbox.core

import akka.http.scaladsl.server.Directives._
import hlouw.akka.http.sandbox.resources.RabbitResource

trait ResourceProvider {
  this: ServiceProvider with Core =>

  lazy val rabbitResource = new RabbitResource(rabbitService)

  lazy val routes = logRequestResult("akka-http-sandbox") {
    rabbitResource.routes
  }
}
