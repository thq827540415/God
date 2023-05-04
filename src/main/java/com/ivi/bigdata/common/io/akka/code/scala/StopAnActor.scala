package com.ivi.bigdata.common.io.akka.code.scala

import akka.actor.{Actor, ActorSystem, Kill, Props}

/**
 * 1. 使用actorSystem.stop(anActor)停止一个actor
 * 2. 在Actor中使用context.stop(childActor)停止一个子Actor
 * 3. context.stop(self)停止自己
 * 4. actor ! PoisonPill发送消息停掉该actor
 * 5. 使用gracefulStop优雅终结actors
 * 6. 发送Kill消息
 */
object StopAnActor extends App {
  val system = ActorSystem("kill")
  private val numbers = system.actorOf(Props[Numbers], "numbers")
  numbers ! "hello"
  numbers ! Kill
}

private class Numbers extends Actor {
  override def receive: Receive = {
    case _ => println("Numbers got a message.")
  }

  override def preStart(): Unit = println("Numbers is alive")

  override def postStop(): Unit = println("Numbers::postStop called")

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    println("Numbers::preRestart called")
  }

  override def postRestart(reason: Throwable): Unit = println("Number::postRestart called")
}
