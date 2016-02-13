package hlouw.akka.http.sandbox.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import hlouw.akka.http.sandbox.entities.RabbitEvent
import io.scalac.amqp.Connection
import org.mongodb.scala._

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * Created by hlouw on 11/02/2016.
  */
class RabbitStream(amqpConnection: Connection, database: MongoDatabase)
                  (implicit system: ActorSystem, mat: ActorMaterializer, executor: ExecutionContextExecutor) {

  import hlouw.akka.http.sandbox.entities.RabbitProtocol._
  import hlouw.akka.http.sandbox.conversion.ConversionFlows._

  private val boundQueue = for {
    queue <- amqpConnection.queueDeclare()
    bind <- amqpConnection.queueBind(queue = queue.name, exchange = "sandbox", routingKey = "rabbit.#")
  } yield queue

  private val eventsCollection = database.getCollection("events")

  val streamToDB: Future[Source[String, Unit]] = {
    val persistInDB = Flow[Document].mapAsync[String](4) { doc =>
      eventsCollection.insertOne(doc).head() map { _ =>
        "Success"
      } recover {
        case _ => "Failure"
      }
    }

    for {
      queue <- boundQueue
      publisher = amqpConnection.consume(queue.name)
      source = Source.fromPublisher(publisher)
    } yield {
      source.via(deliveryTo[RabbitEvent]).via(toMongoDoc[RabbitEvent]).via(persistInDB)
    }
  }

}
