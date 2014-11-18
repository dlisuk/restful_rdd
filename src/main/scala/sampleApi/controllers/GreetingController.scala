package sampleApi.controllers
import org.scalatra.ScalatraServlet

class GreetingController extends ScalatraServlet {
  get("/") {
    "Hello world"
  }

  get("/:name") {
    val name = params.getOrElse("name", "world")
    "Hello " + name
  }
}

