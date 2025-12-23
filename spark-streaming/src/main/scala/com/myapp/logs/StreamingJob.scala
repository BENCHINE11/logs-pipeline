package com.myapp.logs

import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object StreamingJob {

  private val schema = new StructType()
    .add("ts", StringType)
    .add("host", StringType)
    .add("method", StringType)
    .add("path", StringType)
    .add("status", IntegerType)
    .add("bytes", LongType)
    .add("ip", StringType)
    .add("ua", StringType)
    .add("rt_ms", IntegerType)

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("LogsKafkaToHDFS")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    val kafkaBootstrap = sys.env.getOrElse("KAFKA_BOOTSTRAP", "kafka:9092")
    val topic = sys.env.getOrElse("KAFKA_TOPIC", "logs_raw")
    val outPath = sys.env.getOrElse("HDFS_OUT", "hdfs://namenode:8020/datalake/logs/curated")
    val checkpoint = sys.env.getOrElse("CHECKPOINT", "hdfs://namenode:8020/datalake/logs/_chk/curated")

    val raw = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBootstrap)
      .option("subscribe", topic)
      .option("startingOffsets", "latest")
      .load()

    val parsed = raw.selectExpr("CAST(value AS STRING) AS json")
      .select(from_json(col("json"), schema).as("e"))
      .select("e.*")
      .withColumn("event_ts", to_timestamp(col("ts")))
      .drop("ts")
      .withColumn("dt", date_format(col("event_ts"), "yyyy-MM-dd"))
      .withColumn("hour", date_format(col("event_ts"), "HH"))
      .filter(col("event_ts").isNotNull && col("host").isNotNull && col("path").isNotNull)

    // Transformations + window stats (1 minute)
    val stats = parsed
      .withWatermark("event_ts", "2 minutes")
      .groupBy(
        window(col("event_ts"), "1 minute"),
        col("host"),
        col("path")
      )
      .agg(
        count(lit(1)).as("hits"),
        avg(col("rt_ms")).as("avg_rt_ms"),
        sum(when(col("status") >= 500, 1).otherwise(0)).as("errors_5xx")
      )
      .withColumn("window_start", col("window.start"))
      .withColumn("dt", date_format(col("window.start"), "yyyy-MM-dd"))
      .withColumn("hour", date_format(col("window.start"), "HH"))
      .drop("window")

    // ✅ Mini preuve RDD (simple comptage des status codes) — juste pour montrer usage RDD
    val rddProof = parsed.select("status")
      .writeStream
      .foreachBatch { (batch: DataFrame, batchId: Long) =>
        val counts = batch.rdd
          .map(r => Option(r.getAs[Int]("status")).getOrElse(-1))
          .map(s => (s, 1L))
          .reduceByKey(_ + _)
          .collect()
          .sortBy(_._1)

        println(s"[RDD_PROOF] batchId=$batchId status_counts=" + counts.mkString(","))
      }
      .outputMode("append")
      .option("checkpointLocation", checkpoint + "/_rddproof")
      .start()

    val query = stats.writeStream
      .format("parquet")
      .outputMode("append")
      .option("path", outPath)
      .option("checkpointLocation", checkpoint)
      .partitionBy("dt", "hour")
      .start()

    spark.streams.awaitAnyTermination()

  }
}
