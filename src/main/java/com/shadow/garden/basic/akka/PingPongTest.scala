package com.shadow.garden.basic.akka

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object PingPongTest extends App {
  val system = ActorSystem("PingPongSystem")
  private val pong = system.actorOf(Props[Pong], "pong")
  private val ping = system.actorOf(Props(new Ping(pong)), "ping")

  ping ! StartMessage
}


case object PingMessage

case object PongMessage

private case object StartMessage

case object StopMessage

private class Ping(pong: ActorRef) extends Actor {
  private val count = 0

  private def incrementAndPrint(): Unit = {
    context.become(onMessage(count + 1))
    println(s"ping and count: $count")
  }

  override def receive: Receive = onMessage(count)

  private def onMessage(count: Int): Receive = {
    case StartMessage =>
      incrementAndPrint()
      pong ! PingMessage
    case PongMessage =>
      incrementAndPrint()
      if (count > 3) {
        pong ! StopMessage
        println("ping stopped")
        context.stop(self)
      } else {
        pong ! PingMessage
      }
    case _ => println("Pong got something unexpected.")
  }
}

private class Pong extends Actor {
  override def receive: Receive = {
    case PingMessage =>
      println("pong")
      sender ! PongMessage
    case StopMessage =>
      println("pong stopped")
      context.stop(self)
    case _ => println("Pong got something unexpected.")
  }
}
