package hlouw.akka.http.sandbox.entities

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

// Entities

case class RabbitEvent(name: String, message: String) {
  require(name.nonEmpty, "Event name cannot be empty")
}

case class RabbitResponse(outcome: String)


// Protocol

trait RabbitProtocol extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val rabbitResponseFormat = jsonFormat1(RabbitResponse)
  implicit val rabbitEventFormat = jsonFormat2(RabbitEvent)
}

object RabbitProtocol extends RabbitProtocol
