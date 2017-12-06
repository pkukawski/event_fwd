package pl.qki.nms

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.FromConfig
import pl.qki.model.NetworkElement

case class CreateNe(ne: NetworkElement)
case class NeCreated(ne: NetworkElement)
case class DeleteNe(neId: Int)
case class GetNe(neId: Int)
case object GetNes

class MtmActor extends Actor with ActorLogging {

  val neRouter = context.actorOf(Props.empty.withRouter(FromConfig),
    name = "neRouter")

  override def receive: Receive = {
    case other =>
      val replyTo = sender()
      neRouter.tell(other, replyTo)
  }
}
