import javax.servlet.ServletContext
import org.scalatra.LifeCycle

import sampleApi.controllers.GreetingController

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    // Mount servlets.
    context.mount(new GreetingController, "/sample/*")
  }
}
