/*
*  Copyright (c) 2017 ADVA Optical Networking Sp. z o.o.
*  All rights reserved. Any unauthorized disclosure or publication of the confidential and
*  proprietary information to any other party will constitute an infringement of copyright laws.
*
*  Author: Paweł Kukawski, pkukawski@advaoptical.com
*
*  Created: 04/12/2017
*
*  Description:
*/
package pl.qki.nms

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.WebSocketUpgradeResponse
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, WebSocketRequest}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class EventHandlerActor(countersActor: ActorRef) extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val as: ActorSystem = context.system
  implicit val mat: Materializer = ActorMaterializer()

  var webSocketConnection: (Future[WebSocketUpgradeResponse], NotUsed) = _

  override def receive: Receive = {
    case CreateNe(ne) =>
      val baseFlow =
        Flow.fromSinkAndSourceMat(
          Sink.actorRefWithAck[Message](countersActor, Init, Ack, Complete(ne.id)),
          Source.maybe
        )(Keep.left)

      webSocketConnection = Http().singleWebSocketRequest(WebSocketRequest(s"ws://${ne.interface}:${ne.port}/ntfn"), baseFlow)

      webSocketConnection match {
        case (upgradedResponse, _) =>
          val connected = upgradedResponse.map { upgrade =>
            // just like a regular http request we can access response status which is available via upgrade.response.status
            // status code 101 (Switching Protocols) indicates that server support WebSockets
            if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
              Done
            } else {
              throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
            }
          }

          connected.onComplete {
            case Success(_) =>
              log.info(s"Event handler successfully connected to ws://${ne.interface}:${ne.port}/ntfn")
            case Failure(t) => throw t
          }
      }
  }
}

object EventHandlerActor {
  def props(countersActor: ActorRef): Props = Props(new EventHandlerActor(countersActor))
}
