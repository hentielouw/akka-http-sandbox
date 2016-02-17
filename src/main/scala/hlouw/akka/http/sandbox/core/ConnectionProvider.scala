package hlouw.akka.http.sandbox.core

import io.scalac.amqp.Connection
import org.mongodb.scala.MongoClient

/**
  * Created by hlouw on 13/02/2016.
  */
trait ConnectionProvider {

  val amqpConnection = Connection()
  val mongoClient: MongoClient = MongoClient("mongodb://192.168.99.100")
  val database = mongoClient.getDatabase("events")
}
