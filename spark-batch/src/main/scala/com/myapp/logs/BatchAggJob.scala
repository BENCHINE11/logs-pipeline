package com.myapp.logs

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object BatchAggJob {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("LogsBatchAgg")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    val curated = sys.env.getOrElse("CURATED", "hdfs://namenode:8020/datalake/logs/curated")
    val out = sys.env.getOrElse("ANALYTICS", "hdfs://namenode:8020/datalake/logs/analytics")

    // Option: passer dt en arg, sinon dernière dt trouvée (simple fallback: today UTC)
    val dtArg = args.headOption.getOrElse(java.time.LocalDate.now().toString)

    val df = spark.read.parquet(curated)
      .filter(col("dt") === lit(dtArg))

    // hits par minute + taux d’erreur
    val perMinute = df
      .withColumn("minute", date_trunc("minute", col("event_ts")))
      .groupBy(col("minute"))
      .agg(
        count(lit(1)).as("hits"),
        sum(when(col("status") >= 500, 1).otherwise(0)).as("errors_5xx"),
        avg(col("rt_ms")).as("avg_rt_ms")
      )
      .withColumn("error_rate_5xx", col("errors_5xx") / col("hits"))
      .withColumn("dt", date_format(col("minute"), "yyyy-MM-dd"))

    val topPaths = df.groupBy("path")
      .agg(count(lit(1)).as("hits"))
      .orderBy(desc("hits"))
      .limit(50)
      .withColumn("dt", lit(dtArg))

    val topStatus = df.groupBy("status")
      .agg(count(lit(1)).as("hits"))
      .orderBy(desc("hits"))
      .withColumn("dt", lit(dtArg))

    perMinute.write.mode("overwrite").parquet(s"$out/per_minute/dt=$dtArg")
    topPaths.write.mode("overwrite").parquet(s"$out/top_paths/dt=$dtArg")
    topStatus.write.mode("overwrite").parquet(s"$out/top_status/dt=$dtArg")

    spark.stop()
  }
}
