package pl.qki.nms

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import pl.qki.model.{JsonSupport, NetworkElement}

import scala.concurrent.duration._

trait NetworkElementResource extends Directives with JsonSupport {
  implicit val timeout: akka.util.Timeout = 10 seconds

  val actorSystem: ActorSystem
  val mtmProxy: ActorRef

  def routes: Route = pathPrefix("ne") {
    pathEndOrSingleSlash {
      post {
        entity(as[NetworkElement]) { ne =>
          complete {
            mtmProxy ! CreateNe(ne)
            StatusCodes.Created
          }
        }
      }
    }
  }

}
