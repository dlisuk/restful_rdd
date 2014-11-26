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
import scala.collection.mutable

class ScalatraBootstrap extends LifeCycle {
  println("LISUK:Connecting to spark")
  val config = new SparkConf(false).
    setMaster("spark://ion-21-14.sdsc.edu:7077").
    setAppName("ScalatraHost").
    set("spark.executor.memory", "50g")
  val sc = new SparkContext(config)
  sc.addJar("restful_rdd-assembly-0.1-SNAPSHOT.jar")

  override def init(context: ServletContext) {
    // Mount servlets.

    context.mount(new GreetingController, "/sample/*")

    val rddHost = new Servlet()
    context.mount(rddHost, "/rdd/*")
    val rdd = sc.textFile("hdfs://ion-21-14.ibnet0:54310/user/dlisuk/t01").map(x => ProcJson(x)).cache()
    println("LISUK:")
    println(rdd.take(1))
    println("DONE:")
    //val rdd = sc.textFile("/Users/dlisuk/Documents/Projects/restful_rdd/src/main/resources/tweets.json").map(x => ProcJson(x)).cache()
    rddHost.register("tweets", rdd)
  }

  override def destroy(context:ServletContext) {
    sc.stop
  }


}

object ProcJson extends Serializable{
  private val topic_strings:List[String] = List("republican","democrat")
  def apply(rowString:String):Map[String,Any] = {
    val in_format = new SimpleDateFormat("E MMM d H:m:s Z Y", Locale.ENGLISH)
    val out_format = new SimpleDateFormat("yyyyMMddHH", Locale.ENGLISH)

    val row = try {
      parse(rowString)
    }catch {
      case e:Exception => return Map(("ORIG", rowString), ("ERROR", e.getMessage))
    }

    var out_map = mutable.ListBuffer[(String,Any)]()

    out_map += (("orig_date", (row \ "created_at").values.toString))
    try{
      val date =   in_format.parse((row \ "created_at").values.toString)
      out_map += (("date", date.getTime))
    }catch{
      case _:Exception => None
    }
    out_map += (("userid", (row \ "user" \ "id").values.toString))
    out_map += (("username", (row \ "user" \ "name").values.toString))
    out_map += (("text", (row \ "text").values.toString))
    out_map += (("retweets", (row \ "retweet_count").values.toString))
    out_map += (("topics", topic_strings.filter((row \ "text").values.toString.toLowerCase.contains).mkString(" ")))

    out_map.toMap
  }

}
