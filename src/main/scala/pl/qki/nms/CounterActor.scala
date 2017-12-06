package pl.qki.nms

import akka.actor.Actor
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata.{DistributedData, Key, PNCounter, PNCounterKey}
import akka.http.scaladsl.model.ws.TextMessage

import scala.concurrent.duration._

case object Init
case object Ack
case class Complete(id: Long)
case object GetCounters


class CounterActor extends Actor {

  val replicator = DistributedData(context.system).replicator
  implicit val node = Cluster(context.system)

  val CrKey = PNCounterKey.create("cr")
  val MjKey = PNCounterKey.create("mj")

  replicator ! Subscribe(CrKey, self)
  replicator ! Subscribe(MjKey, self)

  import context.dispatcher
  val checkCountersTask = context.system.scheduler.schedule(5.seconds, 5.seconds, self, GetCounters)
  //val readAll = ReadLocal(timeout = 5.seconds)

  override def receive: Receive = {
    case Init =>
      sender() ! Ack

    case TextMessage.Strict(event) =>
      event match {
        case "CR" =>
          replicator ! Update(CrKey, PNCounter.empty.zero, WriteLocal)(_ + 1)
        case "MJ" =>
          replicator ! Update(MjKey, PNCounter.empty.zero, WriteLocal)(_ + 1)
        case _ => //ignore
      }
      sender() ! Ack

    case Complete(id) =>
      println(s"Events flow terminated: $id")

    case g @ GetSuccess(key, _) =>
      println(key.id + " " + g.get(key))

    case c @ Changed(_) => //ignore
    case _: UpdateResponse[_] => //ignore

    case GetCounters =>
      replicator ! Get(CrKey, ReadLocal)
      replicator ! Get(MjKey, ReadLocal)
    case other =>
      println(s"Counter actor received: $other")
  }

  private def incKey[PNCounterKey](key: Key[PNCounter]): Unit = {
    replicator ! Update(key, PNCounter().zero, WriteLocal)(_ + 1)
  }
}
