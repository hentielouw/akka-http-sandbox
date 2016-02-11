package hlouw.akka.http.sandbox.core

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import hlouw.akka.http.sandbox.services.RabbitService
import hlouw.akka.http.sandbox.streams.RabbitStream

import scala.concurrent.ExecutionContextExecutor

/**
  * Created by hlouw on 10/02/2016.
  */
trait Core {

  implicit def system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: ActorMaterializer

  def rabbitService: RabbitService

  def rabbitStream: RabbitStream
}
