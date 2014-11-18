package edu.ucsd.dlisuk.restful_rdd

import org.scalatra.ScalatraServlet
import scala.collection.mutable

class RestfulRdd extends ScalatraServlet{
  private val data_providers = mutable.HashMap[String,DataProvider]()
  data_providers += (("tweets", new DataProvider()))

  get("/:provider/*"){
    data_providers.get(params("provider")).map(_.get(multiParams("splat"))).getOrElse("Bad Data Provider")
  }

}
