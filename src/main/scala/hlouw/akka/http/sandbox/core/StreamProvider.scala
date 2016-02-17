package hlouw.akka.http.sandbox.core

import hlouw.akka.http.sandbox.streams.RabbitStream


trait StreamProvider {
  this: ConnectionProvider with Core =>

  lazy val rabbitStream = new RabbitStream(amqpConnection, database)

  def materializeAll() = {
    rabbitStream.streamToDB.foreach(_.run())
  }
}
