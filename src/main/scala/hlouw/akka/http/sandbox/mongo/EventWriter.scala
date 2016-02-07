package hlouw.akka.http.sandbox.mongo

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.mongodb.CursorType
import io.scalac.amqp.{Delivery, Connection, Message}
import org.mongodb.scala.{Document, MongoDatabase}

/**
  * Created by hlouw on 07/02/2016.
  */
class EventWriter(amqpConnection: Connection, database: MongoDatabase)(implicit system: ActorSystem, mat: ActorMaterializer) {

  import MongoImplicits._

  val eventsDb = Source.fromPublisher(database.getCollection("events").find().cursorType(CursorType.Tailable))
  val eventsQueue = Source.fromPublisher(amqpConnection.consume("events"))

  val consoleSink = Sink.foreach(println)
  val publishEvent = Sink.fromSubscriber(amqpConnection.publishDirectly("events"))

  val documentToMessage = Flow[Document].map { doc =>
    Message(body = doc.toJson().map(_.toByte))
  }

  val deliveryToString = Flow[Delivery].map { delivery =>
    delivery.message.body.map(_.toChar).mkString
  }

  eventsQueue.via(deliveryToString).to(consoleSink).run()
  eventsDb.via(documentToMessage).to(publishEvent).run()
}
