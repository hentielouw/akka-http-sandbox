package hlouw.akka.http.sandbox.services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision}
import hlouw.akka.http.sandbox.conversions.MongoImplicits
import hlouw.akka.http.sandbox.entities.RabbitEvent
import io.scalac.amqp.{Connection, Message}
import org.mongodb.scala.{Document, MongoDatabase}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}


class RabbitService(amqpConnection: Connection, database: MongoDatabase)
                   (implicit system: ActorSystem, mat: ActorMaterializer) {

  import MongoImplicits._
  import hlouw.akka.http.sandbox.entities.RabbitProtocol._

  private val documentToJson = Flow[Document].map[JsValue](_.toJson().parseJson)
  private val unmarshalToEvent = Flow[JsValue].map[RabbitEvent](_.convertTo[RabbitEvent])
  private val eventsCollection = database.getCollection("events")

  val streamFromDB: Source[RabbitEvent, NotUsed] = {
    val dbEventSource: Source[Document, NotUsed] = Source
      .fromPublisher(eventsCollection.find().collect())
      .mapConcat(_.toList)

    dbEventSource
      .via(documentToJson)
      .via(unmarshalToEvent)
      .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))
  }

  def eventsFromDB = streamFromDB.runWith(Sink.seq)

  def postToQueue(event: RabbitEvent)(implicit ec: ExecutionContext): Future[Unit] = {
    val source = Source.single(event)
    val eventsQueueSink = Sink.fromSubscriber(amqpConnection.publish(exchange = "sandbox", routingKey = "rabbit.events"))

    val convertToMessage = Flow[RabbitEvent].map[Message] { event =>
      Message(body = event.toJson.toString.getBytes)
    }

    val post = source
      .via(convertToMessage)
      .runWith(eventsQueueSink)

    Future.successful(())
  }


}
