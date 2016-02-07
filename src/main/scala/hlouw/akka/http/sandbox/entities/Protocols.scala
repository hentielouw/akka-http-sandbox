package hlouw.akka.http.sandbox.entities

import spray.json.DefaultJsonProtocol

trait Protocols extends DefaultJsonProtocol {
  implicit val ipInfoFormat = jsonFormat5(IpInfo.apply)
  implicit val ipPairSummaryRequestFormat = jsonFormat2(IpPairSummaryRequest.apply)
  implicit val ipPairSummaryFormat = jsonFormat3(IpPairSummary.apply)
  implicit val rabbitResponseFormat = jsonFormat1(RabbitResponse.apply)
  implicit val readingsResponseFormat = jsonFormat1(Readings.apply)
}
