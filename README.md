# 📘 Logs Pipeline – Big Data / Data Engineering
## 📌 Description du projet

Ce projet met en œuvre un pipeline Big Data complet pour le traitement et l’analyse de logs applicatifs en temps réel et en batch.

Il simule un cas réel de Data Engineering, depuis la génération des logs jusqu’à la production d’indicateurs analytiques exploitables, en s’appuyant sur les technologies suivantes :

- Apache Kafka : ingestion des données en temps réel
- Apache Spark : traitement streaming et batch
- HDFS : stockage distribué (Data Lake)
- Docker & Docker Compose : orchestration de l’infrastructure

## 🏗️ Architecture globale
```bash
Python Producer
↓
Kafka (logs_raw)
↓
Spark Structured Streaming
↓
HDFS (curated)
↓
Spark Batch Analytics
↓
HDFS (analytics)
```

## ✅ Prérequis

Avant de lancer le projet, assure-toi d’avoir les éléments suivants installés :

### 🔧 Outils système

- Docker ≥ 24.x
- Docker Compose ≥ 2.x
- Git

### 🐍 Python

- Python 3.10+
- Pip installé

### 🧠 Connaissances recommandées

- Bases de Kafka, Spark et Hadoop
- Utilisation du terminal (PowerShell / Bash)

## 📂 Structure du projet
```bash
logs-pipeline/
│
├── docker-compose.yml
│
├── producer/
│   └── produce_logs.py
│
├── spark-streaming/
│   └── src/main/scala/com/myapp/logs/StreamingJob.scala
│
├── spark-batch/
│   └── src/main/scala/com/myapp/logs/BatchAnalytics.scala
│
├── images/                # Screenshots pour le rapport
│
└── README.md
```

## 🚀 Lancement du pipeline (pas à pas)
### 1️⃣ Cloner le projet
```git
git clone https://github.com/BENCHINE11/logs-pipeline.git
cd logs-pipeline
```

### 2️⃣ Lancer l’infrastructure Docker
```bash 
docker compose up -d
```

Vérifier que tous les services sont actifs :
```bash
docker ps
```


Tu dois voir au minimum :

- kafka
- zookeeper
- spark
- namenode
- datanode

### 3️⃣ Créer le topic Kafka
```bash
docker exec -it kafka kafka-topics \
--bootstrap-server kafka:9092 \
--create \
--topic logs_raw \
--partitions 3 \
--replication-factor 1
```

Lister les topics :
```bash
docker exec -it kafka kafka-topics \
--bootstrap-server kafka:9092 \
--list
```

### 4️⃣ Installer les dépendances Python (host)
```bash 
pip install kafka-python
```

### 5️⃣ Lancer le producer (machine hôte)

⚠️ Kafka expose le port 29092 pour les clients externes.
```bash
export KAFKA_BOOTSTRAP=localhost:29092
python producer/produce_logs.py
```

Tu dois voir :
```matlab
Producing to logs_raw @ XXX events/sec
```

### 6️⃣ Vérifier la consommation Kafka (Docker)
```bash 
docker exec -it kafka kafka-console-consumer \
--bootstrap-server kafka:9092 \
--topic logs_raw \
--from-beginning
```

### 7️⃣ Lancer Spark Streaming

Compiler le projet Scala (si nécessaire) :

```bash
cd spark-streaming
sbt clean package
```


Soumettre le job :

```bash
docker exec -it spark bash -lc "
/opt/spark/bin/spark-submit \
--master local[*] \
--packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.1 \
--class com.myapp.logs.StreamingJob \
/app/spark-streaming/target/scala-2.12/*.jar
"
```

### 8️⃣ Vérifier les données dans HDFS
```bash
docker exec -it namenode hdfs dfs -ls /datalake/logs
docker exec -it namenode hdfs dfs -ls /datalake/logs/curated
```

## 📊 Traitement Batch et visualisation
### 9️⃣ Lancer BatchAnalytics
```bash
cd spark-batch
sbt clean package
```
```bash
docker exec -it spark bash -lc "
/opt/spark/bin/spark-submit \
--master local[*] \
--class com.myapp.logs.BatchAnalytics \
/app/spark-batch/target/scala-2.12/*.jar
"
```

### 🔟 Vérifier les résultats analytiques
```bash
docker exec -it namenode hdfs dfs -ls /datalake/logs/analytics
```

Tu dois voir :
- `top_paths`
- `kpi_by_hour`
- `kpi_by_host`
- `top_paths_csv`

### 🔍 Visualiser les résultats (Spark Shell)
```bash
docker exec -it spark /opt/spark/bin/spark-shell
```

Dans le prompt Scala :

```scala
val df = spark.read.parquet("hdfs://namenode:8020/datalake/logs/analytics/top_paths")
df.show(20, false)
df.printSchema()
```
## 🧪 Résultats produits

- Logs traités en temps réel
- Données stockées en Parquet
- KPI globaux :
  - Top endpoints
  - Trafic par heure
  - Erreurs serveur
  - Temps de réponse moyen

## 🔧 Améliorations possibles

- Ajout d’une couche de visualisation (Grafana, Superset)
- Intégration de Delta Lake / Iceberg
- Déploiement cloud (AWS, Azure, GCP)
- Monitoring avec Prometheus & Grafana

## 👤 Auteur

### **Abdelilah BENCHINE**
Étudiant en Génie Informatique – ENSA Tanger <br/>
Projet réalisé dans le cadre du module **Big Data**

## ⭐ Remarque finale

Ce projet a été conçu à des fins pédagogiques afin de démontrer une architecture Big Data complète et réaliste. <br/>
Toute contribution ou amélioration est la bienvenue.