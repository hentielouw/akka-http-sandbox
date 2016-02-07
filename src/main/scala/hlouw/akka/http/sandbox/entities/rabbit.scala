package hlouw.akka.http.sandbox.entities

case class RabbitEvent(name: String, message: String)

case class RabbitResponse(outcome: String)
