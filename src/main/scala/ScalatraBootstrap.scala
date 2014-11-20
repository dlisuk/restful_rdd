import edu.ucsd.dlisuk.restful_rdd.Servlet
import javax.servlet.ServletContext
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.scalatra.LifeCycle

import sampleApi.controllers.GreetingController

import java.text.SimpleDateFormat
import java.util.Locale
import org.json4s._
import org.json4s.jackson.JsonMethods._

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    // Mount servlets.
    val config = new SparkConf(false).
      setMaster("spark://ion-21-14.sdsc.edu:7077").
      setAppName("ScalatraHost")
    val sc = new SparkContext(config)

    context.mount(new GreetingController, "/sample/*")

    val rddHost = new Servlet()
    context.mount(rddHost, "/rdd/*")
    val rdd = sc.textFile("hdfs://ion-21-14.ibnet0:54310/user/dlisuk/t01").map(x => ProcJson(x)).cache()
    rdd.take(1)
    rddHost.register("tweets", rdd)
  }

}

object ProcJson extends Serializable{
  private val topic_strings:List[String] = List("republican","democrat")
  def apply(rowString:String):Map[String,Any] = {
    val in_format = new SimpleDateFormat("E MMM d H:m:s Z Y", Locale.ENGLISH)
    val out_format = new SimpleDateFormat("yyyyMMddHH", Locale.ENGLISH)
    val row = parse(rowString)
    val date =   in_format.parse((row \ "created_at").values.toString)
    val date_string = out_format.format(date).toLong
    val userid = (row \ "user" \ "id").values.toString
    val username = (row \ "user" \ "name").values.toString
    val text = (row \ "text").values.toString
    val retweets = (row \ "retweet_count").values.toString
    val topics = topic_strings.filter(text.toLowerCase.contains).mkString(" ")

    Map(("date", date_string), ("userid",userid), ("username",username),("text",text),("retweets",retweets), ("topics",topics))
  }

}
