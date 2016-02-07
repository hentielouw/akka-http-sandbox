package hlouw.akka.http.sandbox

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import hlouw.akka.http.sandbox.events.EventListener
import hlouw.akka.http.sandbox.resources.{RabbitResource, IpResource}
import io.scalac.amqp.Connection
import akka.http.scaladsl.server.Directives._

trait Service extends IpResource with RabbitResource {

  val routes = logRequestResult("akka-http-microservice") {
    ipRoutes ~ rabbitRoutes
  }
}

object AkkaHttpMicroservice extends App with Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  val amqpConnection = Connection()

  val events = new EventListener(amqpConnection, "customers")

  events.stream.run

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
