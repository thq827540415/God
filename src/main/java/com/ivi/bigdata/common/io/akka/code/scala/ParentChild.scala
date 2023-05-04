package com.ivi.bigdata.common.io.akka.code.scala

import akka.actor.{Actor, ActorSystem, PoisonPill, Props}

object ParentChild extends App {
  val system = ActorSystem("ParentChild")
  val parent = system.actorOf(Props[Parent], "parent")

  parent ! CreateChild("Jonathan")
  parent ! CreateChild("Jordan")

  Thread.sleep(500)

  println("Sending Jonathan a PoisonPill ...")
  private val jonathan = system.actorSelection("/user/parent/Jonathan")
  jonathan ! PoisonPill
  println("jonathan was killed")

  Thread.sleep(5000)
  // system.terminate()
}

private case class CreateChild(name: String)
private case class Name(name: String)

private class Parent extends Actor {
  override def receive: Receive = {
    case CreateChild(name) =>
      println(s"Parent about to create Child ($name)")
      val child = context.actorOf(Props[Child], s"$name")
      child ! Name(name)
    case _ => println("Parent got some other message.")
  }
}

class Child extends Actor {
  var name = "No name"
  override def postStop(): Unit = {
    // self.path = akka://ParentChild/user/parent/Jonathan
    println(s"D'oh! They killed me ($name): ${self.path}")
  }

  override def receive: Receive = {
    case Name(name) => this.name = name
    case _ => println(s"Child $name got some other message.")
  }
}