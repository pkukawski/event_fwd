package pl.qki

import akka.actor.ActorSystem
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Framing, Sink, Tcp}
import akka.util.ByteString
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.util.{Failure, Success}

object EventFwdApp extends App {
  implicit val system = ActorSystem("nms")
  implicit val materializer = ActorMaterializer()

  implicit val ec = system.dispatcher

  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers("localhost:9092")

  lazy val kafkaProducer = producerSettings.createKafkaProducer()

  val TopicName = "events"
  val NumOfPartition = 3

  val echo = Flow[ByteString]
    .via(Framing.delimiter(
      ByteString("\n"),
      maximumFrameLength = 256,
      allowTruncation = true))
    .map(_.utf8String)
    .map { s =>
      println("RECEIVED: " + s)
      val list = s.split(",").toList
      val neHash = list.head.hashCode % NumOfPartition
      ProducerMessage.Message(new ProducerRecord[Array[Byte], String](TopicName, neHash, null, list.last), None)
    }
    .via(Producer.flow(producerSettings, kafkaProducer))
    .map { result =>
      val record = result.message.record
      println(s"${record.topic}/${record.partition} ${result.offset}: ${record.value}" +
        s"(${result.message.passThrough})")
      result
    }
    .map(_ => "OK\n")
    .map(ByteString(_))

  val handler = Sink.foreach[Tcp.IncomingConnection] { conn =>
    println("Client connected from: " + conn.remoteAddress)
    conn handleWith echo
  }

  val port = 6001
  val address = "127.0.0.1"

  val connections = Tcp().bind(address, port)
  val binding = connections.to(handler).run()

  binding.onComplete {
    case Success(b) =>
      println("Server started, listening on: " + b.localAddress)
    case Failure(e) =>
      println(s"Server could not bind to $address:$port: ${e.getMessage}")
      system.terminate()
  }

  sys.ShutdownHookThread {
    println("Terminating...")
    system.terminate()
  }
}