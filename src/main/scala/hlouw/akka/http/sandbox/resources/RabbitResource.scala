package hlouw.akka.http.sandbox.resources

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import hlouw.akka.http.sandbox.entities.RabbitEvent
import hlouw.akka.http.sandbox.services.RabbitService

import scala.concurrent.ExecutionContextExecutor


class RabbitResource(service: RabbitService)
                    (implicit system: ActorSystem, executor: ExecutionContextExecutor, materializer: Materializer) {

  import hlouw.akka.http.sandbox.entities.RabbitProtocol._

  lazy val routes = pathPrefix("rabbit") {
    pathPrefix("events") {
      (post & path(Segment)) { name: String =>
        onSuccess(service.postToQueue(RabbitEvent(name, s"Message for $name"))) {
          complete(StatusCodes.NoContent)
        }
      } ~
      (get & pathEnd) {
        complete(service.eventsFromDB)
      }
    }
  }
}