package hlouw.akka.http.sandbox.resources

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import hlouw.akka.http.sandbox.entities.RabbitEvent
import hlouw.akka.http.sandbox.services.RabbitService

import scala.concurrent.{Future, ExecutionContextExecutor}


class RabbitResource(service: RabbitService)
                    (implicit system: ActorSystem, executor: ExecutionContextExecutor, materializer: Materializer) {

  import hlouw.akka.http.sandbox.entities.RabbitProtocol._

  val routes = pathPrefix("rabbit") {
    pathPrefix("events") {
      (post & pathEnd) {
        onSuccess(service.postToQueue(RabbitEvent("rabbit1", "All rabbits welcome"))) {
          complete(StatusCodes.NoContent)
        }
      } ~
      (get & pathEnd) {
        complete {
          val result: Future[Seq[RabbitEvent]] = service.streamFromDB.runWith(Sink.seq)
          result
        }
      }
    }
  }

}
