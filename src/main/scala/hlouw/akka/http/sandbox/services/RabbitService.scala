package hlouw.akka.http.sandbox.services

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision}
import com.mongodb.CursorType
import hlouw.akka.http.sandbox.entities.RabbitEvent
import io.scalac.amqp.{Connection, Message}
import org.mongodb.scala.{Document, MongoDatabase}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}


class RabbitService(amqpConnection: Connection, database: MongoDatabase)
                   (implicit system: ActorSystem, mat: ActorMaterializer) {

  import MongoImplicits._
  import hlouw.akka.http.sandbox.entities.RabbitProtocol._

  private val unmarshalToEvent = Flow[String]
    .map[RabbitEvent](_.parseJson.convertTo[RabbitEvent])
    .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))

  val streamFromDB: Source[RabbitEvent, Unit] = {
    val eventsCollection = database.getCollection("events")
    val eventsDbSource: Source[Document, Unit] = Source.fromPublisher(eventsCollection.find().first())
    val documentToJson: Flow[Document, String, Unit] = Flow[Document].map[String](_.toJson())

    eventsDbSource.via(documentToJson).via(unmarshalToEvent)
  }

  def postToQueue(event: RabbitEvent)(implicit ec: ExecutionContext): Future[Unit] = {
    val source = Source.single(event)
    val eventsQueueSink = Sink.fromSubscriber(amqpConnection.publishDirectly(queue = "events"))

    val convertToMessage = Flow[RabbitEvent].map[Message] { event =>
      Message(body = event.toString.getBytes)
    }

    val post = source
      .via(convertToMessage)
      .runWith(eventsQueueSink)

    Future.successful(())
  }


}
