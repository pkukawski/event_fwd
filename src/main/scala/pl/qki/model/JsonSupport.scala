package pl.qki.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val neFormat = jsonFormat3(NetworkElement)
  implicit val monotypeFormat = jsonFormat2(Monotype)
  implicit val pmFormat = jsonFormat3(Pm)
}