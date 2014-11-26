import sbtassembly.Plugin.{PathList, MergeStrategy, AssemblyKeys}
import AssemblyKeys._ // put this at the top of the file

assemblySettings


mergeStrategy in assembly <<= (mergeStrategy in assembly) {
  (old) => {
    case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
    case PathList("javax", "transaction", xs @ _*)     => MergeStrategy.first
    case PathList("javax", "mail", xs @ _*)     => MergeStrategy.first
    case PathList("javax", "activation", xs @ _*)     => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
    case "application.conf" => MergeStrategy.concat
    case "META-INF/ECLIPSEF.RSA" => MergeStrategy.first
    case x if x.contains("org/apache/commons/logging") => MergeStrategy.last
    case "plugin.properties" => MergeStrategy.discard
    case x => old(x)
  }
}

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % "2.2.2",
  "org.eclipse.jetty" % "jetty-webapp" % "8.1.7.v20120910" % "container,compile",
 // "org.eclipse.jetty" % "jetty-webapp" % "7.6.9.v20130131" % "container,compile",
  "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016",
  "org.scalatra" %% "scalatra-json" % "2.3.0",
  "org.json4s"   %% "json4s-jackson" % "3.2.9"
)

libraryDependencies ++= Seq(
  ("org.apache.spark"%%"spark-core"%"1.1.0").
    exclude("org.eclipse.jetty.orbit", "javax.servlet").
    exclude("org.eclipse.jetty.orbit", "javax.transaction").
    exclude("org.eclipse.jetty.orbit", "javax.mail").
    exclude("org.eclipse.jetty.orbit", "javax.activation").
    exclude("commons-beanutils", "commons-beanutils-core").
    exclude("commons-collections", "commons-collections").
    exclude("commons-collections", "commons-collections").
    exclude("com.esotericsoftware.minlog", "minlog")
)



Seq(webSettings :_*)

