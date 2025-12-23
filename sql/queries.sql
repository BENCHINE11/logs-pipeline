-- À exécuter dans spark-sql (si besoin) ou via spark-shell/sql
-- Ici on se base sur le parquet partitionné dt/hour

-- Exemple: créer une vue temporaire (si tu utilises spark-shell)
-- CREATE TEMP VIEW logs_curated USING parquet OPTIONS (path 'hdfs://namenode:8020/datalake/logs/curated');

-- Top endpoints (multi partitions)
SELECT path, count(*) AS hits
FROM logs_curated
WHERE dt='2025-12-22'
GROUP BY path
ORDER BY hits DESC
    LIMIT 10;

-- Erreurs 4xx / 5xx par minute
SELECT
    date_trunc('minute', event_ts) AS minute,
  sum(CASE WHEN status BETWEEN 400 AND 499 THEN 1 ELSE 0 END) AS errors_4xx,
  sum(CASE WHEN status BETWEEN 500 AND 599 THEN 1 ELSE 0 END) AS errors_5xx
FROM logs_curated
WHERE dt='2025-12-22'
GROUP BY date_trunc('minute', event_ts)
ORDER BY minute;

-- Latence moyenne par host + status
SELECT host, status, avg(rt_ms) AS avg_rt_ms
FROM logs_curated
WHERE dt='2025-12-22'
GROUP BY host, status
ORDER BY avg_rt_ms DESC;

-- Heavy query: plusieurs partitions dt+hours (ex)
SELECT host, count(*) AS hits, avg(rt_ms) AS avg_rt_ms
FROM logs_curated
WHERE dt='2025-12-22' AND hour IN ('10','11','12','13','14')
GROUP BY host
ORDER BY hits DESC;
