package hlouw.akka.http.sandbox.core

import akka.stream.scaladsl.Sink

/**
  * Created by hlouw on 11/02/2016.
  */
trait Streams {
  this: Core =>

  def materializeAll() = {
    rabbitStream.streamToDB.runWith(Sink.ignore)
  }
}
