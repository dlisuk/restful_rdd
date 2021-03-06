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
    datasets.keys.mkString("\n")
  }

  get("/data/:id"){
    val id = params("id")
    responses.get(id).map(_.value) match{
      case None => NotFound()
      case Some(None) => Accepted()
      case Some(Some(Success(result))) => {
        contentType = "application/json; charset=utf-8"
        Ok(result)
      }
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
        val filter: Map[String, Any] => Boolean = new DcDashboardFilter(params("filter"))

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
        case e:Exception => InternalServerError(reason = e.getMessage)
      }
    }catch{
      case e:Exception => NotFound()
    }
  }

}
