# Logs Pipeline (Kafka + Spark + HDFS + Airflow + Streamlit)

## Run
```bash
docker compose up -d
```

## Create topic
```bash
docker exec -it kafka bash -lc "kafka-topics.sh --bootstrap-server kafka:9092 --create --topic logs_raw --partitions 3 --replication-factor 1"
```

## HDFS init

```bash
docker exec -it namenode bash -lc "hdfs dfs -mkdir -p /datalake/logs/curated /datalake/logs/analytics /datalake/logs/_chk/curated"
```

## Build + Run Streaming

```bash 
docker exec -it spark bash -lc "cd /app/spark-streaming && sbt clean package"
docker exec -it spark bash -lc "/opt/spark/bin/spark-submit --class com.myapp.logs.StreamingJob --master local[*] /app/spark-streaming/target/scala-2.12/logs-streaming_2.12-0.1.0.jar"
```

## Build + Run Batch
```bash
docker exec -it spark bash -lc "cd /app/spark-batch && sbt clean package"
docker exec -it spark bash -lc "/opt/spark/bin/spark-submit --class com.myapp.logs.BatchAggJob --master local[*] /app/spark-batch/target/scala-2.12/logs-batch_2.12-0.1.0.jar 2025-12-22"
```

## Check HDFS
```bash
docker exec -it namenode bash -lc "hdfs dfs -ls -R /datalake/logs/curated | head"
docker exec -it namenode bash -lc "hdfs dfs -ls -R /datalake/logs/analytics | head"
```

## Airflow
Open http://localhost:8080 (admin/admin) and trigger DAG `.logs_pipeline_dag`

---

## Les 3 causes “spaghetti / ça marche plus” (et comment ce setup les évite)

1) **Caches Ivy/Coursier pas montés** ⇒ downloads qui cassent / permissions (`/home/spark/.ivy2/...`)  
✅ Ici on monte `.ivy-cache` sur **/root/.ivy2** ET **/home/spark/.ivy2**.

2) **Tu lances `hdfs dfs -get` vers un dossier local inexistant**  
✅ Ici on fait `mkdir -p /tmp/analytics_local` avant.

3) **Spark UI pas visible / jobs perdus**  
✅ Ici on expose `4040` (Spark UI pendant l’exécution) et on checkpoint dans HDFS.

---

## Si tu fais exactement ça, tu obtiens quoi ?

- `logs_raw` rempli en continu par `producer`
- `StreamingJob` écrit des Parquet partitionnés :  
  `/datalake/logs/curated/dt=YYYY-MM-DD/hour=HH/...parquet`
- `BatchAggJob` écrit :  
  `/datalake/logs/analytics/per_minute/dt=.../` etc.
- Airflow peut déclencher le batch
- Streamlit lit l’export local et te fait 3 graphs + tables

---