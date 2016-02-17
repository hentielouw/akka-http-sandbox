package hlouw.akka.http.sandbox.core

import hlouw.akka.http.sandbox.services.RabbitService

/**
  * Created by hlouw on 10/02/2016.
  */
trait ServiceProvider {
  this: ConnectionProvider with Core =>

  lazy val rabbitService = new RabbitService(amqpConnection, database)
}
