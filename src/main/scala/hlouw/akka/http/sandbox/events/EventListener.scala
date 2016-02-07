package hlouw.akka.http.sandbox.events

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import io.scalac.amqp.{Connection, Delivery}

/**
  * Created by hlouw on 06/02/2016.
  */
class EventListener(amqpConnection: Connection, queue: String)(implicit system: ActorSystem, mat: ActorMaterializer) {

  val stream = {
    val incoming = Source.fromPublisher(amqpConnection.consume(queue = queue))

    val console = Sink.foreach(println)

    val messageExtraction = Flow[Delivery] map { delivery =>
      delivery.message.body.map(_.toChar).mkString
    }

    incoming
      .log("before-extraction")
      .via(messageExtraction)
      .log("after-extraction")
      .to(console)
  }

}
