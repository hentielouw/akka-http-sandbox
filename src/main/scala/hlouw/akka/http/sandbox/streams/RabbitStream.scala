package hlouw.akka.http.sandbox.streams

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision}
import hlouw.akka.http.sandbox.entities.RabbitEvent
import io.scalac.amqp.{Connection, Delivery}
import org.mongodb.scala._
import spray.json._

/**
  * Created by hlouw on 11/02/2016.
  */
class RabbitStream(amqpConnection: Connection, database: MongoDatabase)
                  (implicit system: ActorSystem, mat: ActorMaterializer) {

  import hlouw.akka.http.sandbox.entities.RabbitProtocol._

  val eventsQueueSource = Source.fromPublisher(amqpConnection.consume("events"))
  val eventsCollection = database.getCollection("events")

  private val unmarshalEvent = Flow[String]
    .map[RabbitEvent](_.parseJson.convertTo[RabbitEvent])
    .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))

  val streamToDB: Source[Completed, Unit] = {
    val deliveryToString = Flow[Delivery].map[String] {
      _.message.body.map(_.toChar).mkString
    }

    val persistEventInDB = Flow[RabbitEvent].mapAsync[Completed](4) { event =>
      val doc = Document("name" -> event.name, "message" -> event.message)
      eventsCollection.insertOne(doc).head()
    }

    eventsQueueSource.via(deliveryToString).via(unmarshalEvent).via(persistEventInDB)
  }

}
