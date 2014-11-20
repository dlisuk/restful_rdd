package edu.ucsd.dlisuk.restful_rdd

import org.scalatra.{ScalatraServlet,NotFound,Accepted,Ok,InternalServerError}

import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.{Success,Failure}

import collection.mutable
import org.apache.spark.rdd.RDD
import java.lang.NumberFormatException
import java.util.concurrent.atomic.AtomicInteger

class Servlet() extends ScalatraServlet with Serializable{

  private val idCounter = new AtomicInteger(0)
  private val datasets = mutable.Map[String,RDD[Map[String,Any]]]()
  private val responses = mutable.Map[String,Future[String]]()

  def register(name:String,data:RDD[Map[String,Any]]){
    datasets += ((name, data))

  }

  get("/"){
    compact(JArray(datasets.keys.toList.map(JString(_))))
  }

  get("/data/"){
    contentType="text/html"
    val rows = responses.mapValues(_.value).mapValues {
      case None => "Waiting"
      case Some(Success(_)) => "Ready"
      case Some(Failure(e)) => "Failed"
    }.toList.sortBy(x=> (-x._1.toInt)).map{
      case (id, result) => s"<tr> <td> <a href='./$id'>$id</a> </td> <td> $result </td> <tr>"
    }

    "<table border='1'><tr><th>Data ID</th><th>Status</th></tr>\n" + rows.mkString("\n") + "</table>"
  }

  get("/data/:id"){
    val id = params("id")
    responses.get(id).map(_.value) match{
      case None => NotFound()
      case Some(None) => Accepted()
      case Some(Some(Success(result))) => Ok(result)
      case Some(Some(Failure(exception))) => InternalServerError(exception.getMessage + "\n" + exception.getStackTraceString)
    }
  }

  delete("/data/:id"){
    val id = params("id")
    if(responses.contains(id)) {
      responses -= id
      Ok()
    }else{
      NotFound()
    }
  }

  post("/:dataset"){
    try {
      val dataset = datasets(params("dataset"))
      try {
        val filter: Map[String, Any] => Boolean = _ => true

        val toList: RDD[Map[String, Any]] => List[Map[String, Any]] = try {
          params("method") match {
            case "all" => data => data.toLocalIterator.toList
            case "sample" => {
              val n = params("n").toInt
              data => data.takeSample(false, n).toList
            }
            case "take" => {
              val n = params("n").toInt
              data => data.take(n).toList
            }
            case _ => throw new Exception("Bad method, valid choices are: 'all', 'take', or 'sample'")
          }
        } catch {
          case e: NumberFormatException => throw new Exception("n must be an integer")
        }

        val futureFunction: ()=>String = ()=>{
          val localData = toList(dataset.filter(filter)).map(Extraction.decompose(_)(DefaultFormats))
          compact(JArray(localData))
        }
        val id = idCounter.incrementAndGet().toString
        responses += ((id, Future(futureFunction())))
        Ok(id)
      }catch{
        case e:Exception => InternalServerError(e.getMessage)
      }
    }catch{
      case e:Exception => NotFound()
    }
  }

}
