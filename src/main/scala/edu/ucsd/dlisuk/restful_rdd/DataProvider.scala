package edu.ucsd.dlisuk.restful_rdd

import scala.io.Source

class DataProvider {
  private val tweets = "["+Source.fromFile("/Users/dlisuk/Documents/Projects/restful_rdd/src/main/resources/tweets.json").getLines().mkString(",")+"]"

  def get(params:Seq[String]):String = {
    tweets
  }
}
