package pl.qki.ne

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.ConfigFactory
import pl.qki.model.{JsonSupport, Monotype, Pm}

import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.Random

object NeApp extends App with JsonSupport {
  val config = ConfigFactory.load().getConfig("ne")
  implicit val actorSystem = ActorSystem("ne-system", config)
  implicit val flowMaterializer = ActorMaterializer()
  import actorSystem.dispatcher

  val interface = "localhost"
  val port = if(args.isEmpty) 8080 else args(0).toInt
  val EventFreqMillis = 100

  val Alarms = List("CR", "MJ" /*, "MN", "WARN"*/).toArray

  private def randomEvent: String = {
    Alarms.apply(Math.abs(Random.nextInt()) % Alarms.size)
  }

  val pms: List[Pm] = (1 to 1000).map(i => Pm(System.currentTimeMillis(), "ok", List(Monotype("a", i), Monotype("b", i*2)))).toList

  val ntfnFlow = Source
    .tick(1 seconds, EventFreqMillis millisecond, ())
    .map(_ => randomEvent)
    .map(TextMessage(_))

  val route = get {
    pathEndOrSingleSlash {
      complete(s"Network Elemenet on $interface $port")
    }
  } ~
    path("ntfn") {
      handleWebSocketMessages(Flow.fromSinkAndSourceCoupled(Sink.ignore, ntfnFlow))
    } ~ path("pm") {
    get {
      complete(pms)
    }
  }

  val binding = Http().bindAndHandle(route, interface, port)
  println(s"NE is now online at ws://$interface:$port/ntfn\nPress RETURN to stop...")
  StdIn.readLine()

  binding.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())
  println("NE is down...")
}
