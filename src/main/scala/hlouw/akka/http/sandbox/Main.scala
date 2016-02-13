package hlouw.akka.http.sandbox

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import hlouw.akka.http.sandbox.core._

object Main extends App
  with Connections
  with Services
  with Resources
  with Streams
  with Core {

  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  val config = ConfigFactory.load()

  materializeAll()

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
