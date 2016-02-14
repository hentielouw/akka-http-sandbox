package hlouw.akka.http.sandbox.conversions

import akka.stream.scaladsl.Flow
import io.scalac.amqp.{Delivery, Message}
import org.mongodb.scala._

trait ConversionFlows {
  import spray.json._

  val mongodbToJson = Flow[Document].map[JsValue](_.toJson().parseJson)

  val stringToMongoDoc = Flow[String].map[Document](json => Document(json))

  val deliveryToJson = Flow[Delivery].map[JsValue] {
    _.message.body.map(_.toChar).mkString.parseJson
  }

  val jsonToMessage = Flow[JsValue]
    .map[Message] { json =>
      Message(body = json.toString.getBytes)
    }

  def unmarshalTo[T](implicit format: JsonFormat[T]): Flow[JsValue, T, Unit] = Flow[JsValue].map[T](_.convertTo[T])

  def deliveryTo[T](implicit format: JsonFormat[T]): Flow[Delivery, T, Unit] = deliveryToJson.via(unmarshalTo[T])

  def toMongoDocFrom[T](implicit format: JsonFormat[T]): Flow[T, Document, Unit] = Flow[T]
    .map[String](entity => entity.toJson.toString)
    .via(stringToMongoDoc)
}

object ConversionFlows extends ConversionFlows
