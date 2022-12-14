package com.ava.bigdata.common.io.akka.iquery

package object demo {
  case class Connect(workerId: String, workerCores: Int, workerMemory: Int, var ts: Long)
  case object Success
  case class MyHeartbeat(workerId: String)
}
