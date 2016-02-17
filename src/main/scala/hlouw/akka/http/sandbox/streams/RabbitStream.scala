package hlouw.akka.http.sandbox.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision}
import hlouw.akka.http.sandbox.entities.RabbitEvent
import io.scalac.amqp.{Connection, Queue}
import org.mongodb.scala._

import scala.concurrent.{ExecutionContextExecutor, Future}


class RabbitStream(amqpConnection: Connection, database: MongoDatabase)
                  (implicit system: ActorSystem, mat: ActorMaterializer, executor: ExecutionContextExecutor) {

  import hlouw.akka.http.sandbox.conversions.ConversionFlows._
  import hlouw.akka.http.sandbox.entities.RabbitProtocol._

  private val queueDef = Queue("myqueue", durable = true)

  private val boundQueue = for {
    _ <- amqpConnection.queueDeclare(queueDef)
    bind <- amqpConnection.queueBind(queue = queueDef.name, exchange = "sandbox", routingKey = "rabbit.#")
  } yield queueDef

  private val eventsCollection = database.getCollection("events")

  val streamToDB: Future[Source[String, NotUsed]] = {
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
      source
        .via(deliveryTo[RabbitEvent])
        .via(toMongoDocFrom[RabbitEvent])
        .via(persistInDB)
        .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))
    }
  }

}
