package hlouw.akka.http.sandbox

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import hlouw.akka.http.sandbox.events.EventListener
import hlouw.akka.http.sandbox.mongo.EventWriter
import hlouw.akka.http.sandbox.resources.{RabbitResource, IpResource}
import io.scalac.amqp.Connection
import akka.http.scaladsl.server.Directives._
import org.mongodb.scala.MongoClient

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
  val mongoClient: MongoClient = MongoClient("mongodb://192.168.99.100")
  val database = mongoClient.getDatabase("events")

  val events = new EventListener(amqpConnection, "customers")
  events.stream.run

  val dbStuff = new EventWriter(amqpConnection, database)

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
