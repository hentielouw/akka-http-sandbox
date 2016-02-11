package hlouw.akka.http.sandbox

import akka.event.NoLogging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Flow
import hlouw.akka.http.sandbox.core.Resource
import hlouw.akka.http.sandbox.entities.IpPairSummary
import hlouw.akka.http.sandbox.entities.{IpPairSummary, IpPairSummaryRequest, IpInfo}
import org.scalatest._

class RabbitSpec extends FlatSpec with Matchers with ScalatestRouteTest with Resource {

  "Service" should "respond to single IP query" in {
    Get(s"/ip/${ip1Info.ip}") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[IpInfo] shouldBe ip1Info
    }

    Get(s"/ip/${ip2Info.ip}") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[IpInfo] shouldBe ip2Info
    }
  }

  it should "respond to IP pair query" in {
    Post(s"/ip", IpPairSummaryRequest(ip1Info.ip, ip2Info.ip)) ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[IpPairSummary] shouldBe ipPairSummary
    }
  }
}