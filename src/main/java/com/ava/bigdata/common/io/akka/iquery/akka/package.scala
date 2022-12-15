package com.ava.bigdata.common.io.akka.iquery

package object akka {
  trait RemoteMessage extends Serializable
  case class Connect(workerId: String, workerCores: Int, workerMemory: Int, var ts: Long) extends RemoteMessage
  case object Success extends RemoteMessage
  case class MyHeartbeat(workerId: String) extends RemoteMessage
}
