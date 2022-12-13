package com.ava.bigdata.common.akka

import akka.actor.{Actor, ActorSystem, Props}

/**
 * 让当前接收消息的方法改变
 */
object BecomeHulkExample extends App {
  val system = ActorSystem("Become")
  private val davidBanner = system.actorOf(Props[DavidBanner], "DavidBanner")

  davidBanner ! ActNormalMessage
  davidBanner ! TryToFindSolution
  davidBanner ! BadGuyMakeMeAngry
  Thread.sleep(1000)
  davidBanner ! ActNormalMessage
}

private case object ActNormalMessage

private case object TryToFindSolution

private case object BadGuyMakeMeAngry

private class DavidBanner extends Actor {
  private def angryState: Receive = {
    case ActNormalMessage =>
      println("Phew, I'm back to being David.")
      context.become(normalState)
  }

  private def normalState: Receive = {
    case TryToFindSolution =>
      println("Looking for solution to my problem ...")
    case BadGuyMakeMeAngry =>
      println("I'm getting angry ...")
      context.become(angryState)
  }

  override def receive: Receive = {
    case BadGuyMakeMeAngry => context.become(angryState)
    case ActNormalMessage => context.become(normalState)
  }
}