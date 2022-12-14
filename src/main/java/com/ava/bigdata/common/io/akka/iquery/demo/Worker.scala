package com.ava.bigdata.common.io.akka.iquery.demo

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object Worker {
  val workerId = "001"
  val host = "localhost"
  val port = 8888
  val configStr: String =
    s"""
       |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
       |akka.actor.warn-about-java-serializer-usage = false
       |akka.remote.netty.tcp.hostname = $host
       |akka.remote.netty.tcp.port = $port
       |""".stripMargin
  def start(): Unit = {
    val worker = ActorSystem("Worker", ConfigFactory.parseString(configStr))
    worker.actorOf(Props[Worker], "worker")

    val master = worker.actorSelection("akka.tcp://Master@localhost:9000/user/master")

    val t = new Thread(() => {
      while (true) {
        Thread.sleep(1000)
        master ! MyHeartbeat(workerId)
      }
    }, "worker heartbeat sender")
    t.setDaemon(true)
    t.start()
  }

  def main(args: Array[String]): Unit = {
    start()
  }
}


private class Worker extends Actor {
  /**
   * 初始化通信，给Master发送注册消息
   */
  override def preStart(): Unit = {
    val master = context.actorSelection("akka.tcp://Master@localhost:9000/user/master")
    master ! Connect(Worker.workerId, 4, 128, System.currentTimeMillis())
  }

  override def receive: Receive = {
    case Success =>
      println("client connect successful")
      sender ! MyHeartbeat("001")
  }
}