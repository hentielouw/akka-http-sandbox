package hlouw.akka.http.sandbox.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import hlouw.akka.http.sandbox.entities.RabbitEvent
import io.scalac.amqp.{Connection, Queue}
import org.mongodb.scala._

import scala.concurrent.ExecutionContextExecutor

sealed trait DbResult
case object DbSuccess extends DbResult
case class DbFailure(t: Throwable) extends DbResult

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

  val persistInDB = Flow[Document].mapAsync[DbResult](4) { doc =>
    eventsCollection.insertOne(doc).head()
      .map(_ => DbSuccess)
      .recover {
        case t => DbFailure(t)
      }
  }.named("dbPersist")

  val streamToDB =
    for {
      queue <- boundQueue
      publisher = amqpConnection.consume(queue.name)
      source = Source.fromPublisher(publisher).via(deliveryTo[RabbitEvent])
//      source = Source.single(RabbitEvent("stub", "to test"))
    } yield mongoDBGraph(source)

  private def mongoDBGraph(eventSource: Source[RabbitEvent, NotUsed]): RunnableGraph[NotUsed] = {

    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val mq            = builder.add(eventSource)
      val persist       = builder.add(toMongoDocFrom[RabbitEvent].via(persistInDB))
      val b             = builder.add(Broadcast[DbResult](2))

      val selectFailure = builder.add(Flow[DbResult].mapConcat[DbFailure] {
        case DbSuccess => Nil
        case DbFailure(t) => DbFailure(t) :: Nil
      })

      val selectSuccess = builder.add(Flow[DbResult].mapConcat[DbSuccess.type] {
        case DbSuccess => DbSuccess :: Nil
        case DbFailure(t) => Nil
      })

      val log           = builder.add(Sink.foreach(println))
      val ignore        = builder.add(Sink.ignore)

      mq ~> persist ~> b ~> selectFailure ~> log
                       b ~> selectSuccess ~> ignore

      ClosedShape
    })
  }

}
