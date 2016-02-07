package hlouw.akka.http.sandbox.resources

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import hlouw.akka.http.sandbox.entities.{Protocols, RabbitEvent}
import io.scalac.amqp.{Connection, Message}

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * Created by hlouw on 07/02/2016.
  */
trait RabbitResource extends Protocols {

  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def amqpConnection: Connection

  val rabbitRoutes = {
    pathPrefix("events") {
      (post & pathEnd) {
        complete(postEvent(RabbitEvent("rabbit1", "All rabbits welcome")))
      }
    }
  }

  def postEvent(event: RabbitEvent): Future[ToResponseMarshallable] = {
    val rabbitMq = Sink.fromSubscriber(amqpConnection.publishDirectly(queue = "customers"))

    val source = Source.single(event)

    val convertToMessage = Flow[RabbitEvent] map { event =>
      Message(body = event.toString.getBytes)
    }

    val stream = source.via(convertToMessage).alsoTo(rabbitMq).runWith(Sink.head)

    stream.map[ToResponseMarshallable] {
      case _ => StatusCodes.OK -> "success"
    }
  }

}
