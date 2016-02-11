package hlouw.akka.http.sandbox.core

import hlouw.akka.http.sandbox.services.RabbitService
import hlouw.akka.http.sandbox.streams.RabbitStream
import io.scalac.amqp.Connection
import org.mongodb.scala.MongoClient

/**
  * Created by hlouw on 10/02/2016.
  */
trait Bootable extends Core {

  val amqpConnection = Connection()
  val mongoClient: MongoClient = MongoClient("mongodb://192.168.99.100")
  val database = mongoClient.getDatabase("events")

  override val rabbitService = new RabbitService(amqpConnection, database)

  override val rabbitStream = new RabbitStream(amqpConnection, database)
}
