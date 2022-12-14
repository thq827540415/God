package com.ava.bigdata.common.io.akka.iquery.demo

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable

object Master {
  val map: mutable.HashMap[String, Connect] = mutable.HashMap[String, Connect]()

  val host = "localhost"
  val port = 9000

  val configStr: String =
    s"""
       |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
       |akka.actor.warn-about-java-serializer-usage = false
       |akka.remote.netty.tcp.hostname = $host
       |akka.remote.netty.tcp.port = $port
       |""".stripMargin

  def start(): Unit = {
    val master: ActorSystem = ActorSystem("Master", ConfigFactory.parseString(configStr))
    master.actorOf(Props[Master], "master")

    val t = new Thread(() => {
      // 定时清理，所有过时的worker
      while (true) {
        Thread.sleep(500)
        map.filter(kv => System.currentTimeMillis() - kv._2.ts > 2000).keySet
          .foreach(map -= _)
        println(map)
      }
    }, "master cleaner")
    t.setDaemon(true)
    t.start()
  }
  def main(args: Array[String]): Unit = {
    start()
  }
}

private class Master extends Actor {
  override def receive: Receive = {
    case conn: Connect =>
      println(s"a client connected and workerID: ${conn.workerId}, " +
        s"workerCores: ${conn.workerCores}, workerMemory: ${conn.workerMemory}")
      Master.map.put(conn.workerId, conn)
      // 发送消息给Worker
      // 使用sender和self不同，sender知道发送端的ip:port，self不知道
      // val notSender = context.actorSelection("akka.tcp://Worker@localhost:8888/user/worker")
      sender ! Success
    case MyHeartbeat(workerId: String) =>
      println(s"接收到心跳 -> $workerId，更新时间戳")
      Master.map(workerId).ts = System.currentTimeMillis()
    case _ => println("Master receive another message")
  }
}
