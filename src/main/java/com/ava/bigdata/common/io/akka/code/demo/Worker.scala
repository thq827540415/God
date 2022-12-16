package com.ava.bigdata.common.io.akka.code.demo

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import java.util.UUID

object Worker {
  val host = "localhost"
  val port = 8888

  val configStr: String =
    s"""
       |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
       |akka.actor.warn-about-java-serializer-usage = false
       |akka.remote.netty.tcp.hostname = $host
       |akka.remote.netty.tcp.port = $port
       |""".stripMargin

  def start(workerId: String, workerCores: Int, workerMemory: Int, masterHost: String, masterPort: Int): Unit = {
    val worker = ActorSystem("Worker", ConfigFactory.parseString(configStr))
    worker.actorOf(Props(new Worker(workerId, workerCores, workerMemory, masterHost, masterPort)), "worker")
    /*

    val master = worker.actorSelection(s"akka.tcp://Master@$masterHost:$masterPort/user/master")
    val t = new Thread(() => {
      while (true) {
        Thread.sleep(1000)
        master ! MyHeartbeat(workerId)
      }
    }, "worker heartbeat sender")
    t.setDaemon(true)
    t.start() */
  }

  def main(args: Array[String]): Unit = {
    start(UUID.randomUUID().toString, 4, 128, "localhost", 9000)
  }
}

private class Worker(
                      workerId: String,
                      workerCores: Int,
                      workerMemory: Int,
                      masterHost: String,
                      masterPort: Int) extends Actor {
  /**
   * 初始化通信，给Master发送注册消息
   */
  override def preStart(): Unit = {
    val master = context.actorSelection(s"akka.tcp://Master@$masterHost:$masterPort/user/master")
    master ! Connect(workerId, workerCores, workerMemory, System.currentTimeMillis())
  }

  override def receive: Receive = {
    case Success =>
      println(s"client $workerId connected successfully")

      import scala.language.postfixOps
      import scala.concurrent.duration._
      import context.dispatcher

      // 定期向Master发送心跳
      context.system.scheduler.schedule(0 nanos, 500 millis, sender, MyHeartbeat(workerId))

      // sender ! MyHeartbeat(workerId)
  }
}