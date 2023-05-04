package com.ivi.bigdata.common.io.akka.code.scala

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props, Terminated}

object Watch extends App {
  val system = ActorSystem("watch")
  val parent = system.actorOf(Props[Parent1], "parent")

  val kenny = system.actorSelection("/user/parent/kenny")
  kenny ! PoisonPill
  Thread.sleep(5000)
}

private class Kenny1 extends Actor {
  override def receive: Receive = {
    case _ => println("Kenny received a message")
  }
}

class Parent1 extends Actor {

  // start Kenny as a child, then keep an eye on it.
  private val kenny: ActorRef = context.actorOf(Props[Kenny1], "kenny")
  // 当kenny停止后，会收到一个Terminated消息
  context.watch(kenny)

  override def receive: Receive = {
    case Terminated(kenny) => println("OMG, they killed kenny")
    case _ => println("Parent received a message")
  }
}