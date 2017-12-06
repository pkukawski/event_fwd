package pl.qki.nms

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy,
  ClusterSingletonProxySettings}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.io.StdIn

object NmsApp extends App {
  val interface = "localhost"
  val port = 9000
  val portAkka = if (args.isEmpty) "2551" else args(0)
  val config = ConfigFactory
    .parseString(s"akka.remote.netty.tcp.port=$portAkka")
    .withFallback(ConfigFactory.load().getConfig("nms"))

  implicit val system = ActorSystem("nms", config)
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val ec = system.dispatcher

  system.actorOf(Props[NeActor], name = "neActor")

  if(portAkka == "2551") {

    val mtmRef = system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = Props(classOf[MtmActor]),
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(system)),
      name = "mtm")

    val neResource = new NetworkElementResource {
      override val actorSystem: ActorSystem = system
      override val mtmProxy = system.actorOf(
        ClusterSingletonProxy.props(
          singletonManagerPath = "/user/mtm",
          settings = ClusterSingletonProxySettings(system)),
        name = "statsServiceProxy")
    }
    val route = pathPrefix("v1") {
      neResource.routes
    }

    val binding = Http().bindAndHandle(route, interface, port)

    system.actorOf(Props[ClusterMonitor], name = "clusterMonitor")

    println(s"NMS is now online at http://$interface:$port/\nPress RETURN to stop...")
    StdIn.readLine()
    binding.flatMap(_.unbind()).onComplete(_ => system.terminate())
  } else if(portAkka == "2552"){
    println(s"NMS is now online on port $portAkka")


    StdIn.readLine()
  }

  system.terminate()
  println("NMS server is down...")
}
