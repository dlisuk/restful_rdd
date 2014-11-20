package edu.ucsd.dlisuk.restful_rdd

import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.collection.mutable
import scala.util.{Success, Try}

class DcDashboardFilter(config:JValue) extends (Map[String,Any]=>Boolean) with Serializable{
  def this(config:String) = this(parse(config))

  override def apply(v1: Map[String, Any]): Boolean = {
    filters.forall(_(v1))
  }

  val filters = for(
    (k,v) <- config.values.asInstanceOf[Map[String, List[Any]]]
    if(!v.isEmpty)
  ) yield {
    val membership: mutable.SetBuilder[String, Set[String]] = new mutable.SetBuilder(Set())
    val ranges: mutable.ListBuffer[(Double, Double)] = new mutable.ListBuffer()

    v collect {
      case x :: y :: _ => ranges += ((x.toString.toDouble, y.toString.toDouble))
      case x           => membership += x.toString
    }

    val m = membership.result()
    val r = ranges.result()
    println(m)
    println(r)

    (rec:Map[String,Any]) => rec.get(k).map(x => (Try(x.toString.toDouble), x.toString)).exists {
      case (Success(d), s) => m.contains(s) || r.exists {
        case (min, max) => min <= d && d <= max
      }
      case (_, s) => m.contains(s)
    }
  }
}
