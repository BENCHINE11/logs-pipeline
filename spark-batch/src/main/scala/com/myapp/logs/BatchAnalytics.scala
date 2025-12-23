package com.myapp.logs

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object BatchAnalytics {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("LogsBatchAnalytics")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    val curated = sys.env.getOrElse("HDFS_CURATED", "hdfs://namenode:8020/datalake/logs/curated")
    val out     = sys.env.getOrElse("HDFS_ANALYTICS", "hdfs://namenode:8020/datalake/logs/analytics")

    val df = spark.read.parquet(curated)
      .filter(col("dt").isNotNull && col("hour").isNotNull)

    // 1) Top paths (sur toute la période)
    val topPaths = df.groupBy(col("path"))
      .agg(sum(col("hits")).as("total_hits"),
        sum(col("errors_5xx")).as("total_5xx"),
        avg(col("avg_rt_ms")).as("avg_rt_ms"))
      .orderBy(desc("total_hits"))
      .limit(50)

    // 2) KPI par heure (dt, hour)
    val kpiByHour = df.groupBy(col("dt"), col("hour"))
      .agg(
        sum(col("hits")).as("hits"),
        sum(col("errors_5xx")).as("errors_5xx"),
        avg(col("avg_rt_ms")).as("avg_rt_ms")
      )
      .orderBy(col("dt"), col("hour"))

    // 3) KPI par host
    val kpiByHost = df.groupBy(col("host"))
      .agg(
        sum(col("hits")).as("hits"),
        sum(col("errors_5xx")).as("errors_5xx"),
        avg(col("avg_rt_ms")).as("avg_rt_ms")
      )
      .orderBy(desc("hits"))

    // Écriture en Parquet (un dossier par dataset)
    topPaths.write.mode("overwrite").parquet(out + "/top_paths")
    kpiByHour.write.mode("overwrite").parquet(out + "/kpi_by_hour")
    kpiByHost.write.mode("overwrite").parquet(out + "/kpi_by_host")

    // Bonus : petit CSV lisible rapidement (coalesce pour 1 fichier)
    topPaths.coalesce(1).write.mode("overwrite").option("header", "true").csv(out + "/top_paths_csv")

    spark.stop()
  }
}
