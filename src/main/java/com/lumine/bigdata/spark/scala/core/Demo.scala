package com.lumine.bigdata.spark.scala.core

object Demo {
  def main(args: Array[String]): Unit = {
    new ZS("zs")
  }

  abstract class Person(name: String) {
    println("hello")
  }

  class ZS(name: String) extends Person(name)
}
