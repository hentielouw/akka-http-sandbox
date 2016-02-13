package hlouw.akka.http.sandbox.services

import akka.actor.ActorSystem
import akka.stream.{Supervision, ActorAttributes, ActorMaterializer}
import akka.stream.scaladsl.{Flow, Sink, Source}
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

  val streamFromDB: Source[RabbitEvent, Unit] = {
    val dbEventSource: Source[Document, Unit] = Source
      .fromPublisher(eventsCollection.find().collect())
      .mapConcat(_.toList)

    dbEventSource
      .via(documentToJson)
      .via(unmarshalToEvent)
      .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))
  }

  def eventsFromDB: Future[Seq[RabbitEvent]] = streamFromDB.runWith(Sink.seq)

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
