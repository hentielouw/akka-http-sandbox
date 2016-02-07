package hlouw.akka.http.sandbox.resources

import java.io.IOException

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.Segment
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.Config
import hlouw.akka.http.sandbox.entities.{IpInfo, IpPairSummary, IpPairSummaryRequest, Protocols}

import scala.concurrent.{ExecutionContextExecutor, Future}

trait IpResource extends Protocols {

  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def config: Config
  val logger: LoggingAdapter

  val ipRoutes = {
    pathPrefix("ip") {
      (get & path(Segment)) { ip =>
        complete(getIpInfo(ip))
      } ~
      (post & entity(as[IpPairSummaryRequest]) & pathEnd) { ipPairSummaryRequest =>
        complete(postSummaryRequest(ipPairSummaryRequest))
      }
    }
  }

  def getIpInfo(ip: String): Future[ToResponseMarshallable] =
    fetchIpInfo(ip).map[ToResponseMarshallable] {
      case Right(ipInfo) => ipInfo
      case Left(errorMessage) => BadRequest -> errorMessage
    }

  def postSummaryRequest(ipPairSummaryRequest: IpPairSummaryRequest): Future[ToResponseMarshallable] = {
    val ip1InfoFuture = fetchIpInfo(ipPairSummaryRequest.ip1)
    val ip2InfoFuture = fetchIpInfo(ipPairSummaryRequest.ip2)
    ip1InfoFuture.zip(ip2InfoFuture).map[ToResponseMarshallable] {
      case (Right(info1), Right(info2)) => IpPairSummary(info1, info2)
      case (Left(errorMessage), _) => BadRequest -> errorMessage
      case (_, Left(errorMessage)) => BadRequest -> errorMessage
    }
  }

  lazy val freeGeoIpConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection(config.getString("services.freeGeoIpHost"), config.getInt("services.freeGeoIpPort"))

  def freeGeoIpRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(freeGeoIpConnectionFlow).runWith(Sink.head)

  def fetchIpInfo(ip: String): Future[Either[String, IpInfo]] = {
    freeGeoIpRequest(RequestBuilding.Get(s"/json/$ip")).flatMap { response =>
      response.status match {
        case OK => Unmarshal(response.entity).to[IpInfo].map(Right(_))
        case BadRequest => Future.successful(Left(s"$ip: incorrect IP format"))
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          val error = s"FreeGeoIP request failed with status code ${response.status} and entity $entity"
          logger.error(error)
          Future.failed(new IOException(error))
        }
      }
    }
  }

}
