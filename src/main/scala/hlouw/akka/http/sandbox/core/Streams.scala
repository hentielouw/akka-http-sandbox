package hlouw.akka.http.sandbox.core

import akka.stream.scaladsl.Sink
import hlouw.akka.http.sandbox.streams.RabbitStream

/**
  * Created by hlouw on 11/02/2016.
  */
trait Streams {
  this: Connections with Core =>

  lazy val rabbitStream = new RabbitStream(amqpConnection, database)

  def materializeAll() = {
    rabbitStream.streamToDB.foreach(_.runWith(Sink.ignore))
  }
}
