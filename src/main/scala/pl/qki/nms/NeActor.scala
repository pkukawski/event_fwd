package pl.qki.nms

import akka.actor.{Actor, ActorLogging, Props}

case object Create

class NeActor extends Actor with ActorLogging {

  private lazy val counters = context.actorOf(Props[CounterActor], name = "counters")
  private lazy val eventHandler = context.actorOf(EventHandlerActor.props(counters), name = "eventHandler")

  override def receive: Receive = {
    case cne @ CreateNe(ne) =>
      log.info(s"Attempt to create $ne")
      eventHandler forward cne

    case NeCreated(ne) =>

  }
}
