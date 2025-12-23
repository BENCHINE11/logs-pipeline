ThisBuild / scalaVersion := "2.12.18"

lazy val root = (project in file("."))
  .settings(
    name := "logs-batch",
    version := "0.1.0",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-sql" % "3.5.1" % "provided"
    )
  )
