package com.ava.bigdata.common.akka

import akka.actor.{Actor, ActorSystem, Props}

object LifecycleTest extends App {
  val system = ActorSystem("Lifecycle")
  val kenny = system.actorOf(Props[Kenny], "Kenny")
  println("sending kenny a simple String message")
  kenny ! "hello"
  Thread.sleep(1000)

  println("make kenny restart")
  kenny ! ForceRestart
  Thread.sleep(1000)

  println("stopping kenny")
  system.stop(kenny)

  println("shutting down system")
  system.terminate()
}


private case object ForceRestart

private class Kenny extends Actor {

  println("enter the Kenny constructor")

  override def preStart(): Unit = println("preStart")

  override def postStop(): Unit = println("postStop")

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    println("preRestart")
    println(s"MESSAGE: ${message.getOrElse("")}")
    println(s"REASON: ${reason.getMessage}")
    super.preRestart(reason, message)
  }

  override def postRestart(reason: Throwable): Unit = {
    println("postRestart")
    println(s"REASON: ${reason.getMessage}")
    super.postRestart(reason)
  }

  override def receive: Receive = {
    case ForceRestart => throw new Exception("Boom!")
    case _ => println("Kenny received a message")
  }
}
