package com.ava.bigdata.common.io.akka

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps


/**
 * 给Actor发消息并等待回复
 */
object AskTest extends App {
  val system = ActorSystem("Ask")
  private val myActor = system.actorOf(Props[TestActor], "myActor")
  implicit val timeout: Timeout = Timeout(5 seconds)
  private val future: Future[Any] = myActor ? AskNameMessage
  // 使用阻塞的方式获取结果
  val result = Await.result(future, timeout.duration).asInstanceOf[String]
  println(result)

  private val future1: Future[String] = ask(myActor, AskNameMessage).mapTo[String]
  private val result1 = Await.result(future1, 1 second)
  println(result1)
}

private case object AskNameMessage

private class TestActor extends Actor {
  override def receive: Receive = {
    case AskNameMessage => sender ! "Fred"
    case _ => println("that was unexpected")
  }
}
