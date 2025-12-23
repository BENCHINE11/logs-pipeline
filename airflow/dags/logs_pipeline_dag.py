from datetime import datetime
from airflow import DAG
from airflow.operators.bash import BashOperator

default_args = {"owner": "airflow"}

with DAG(
        dag_id="logs_pipeline_batch",
        default_args=default_args,
        start_date=datetime(2025, 12, 22),
        schedule=None,        # Airflow 2.4+ (sinon schedule_interval=None)
        catchup=False,
) as dag:

    run_batch = BashOperator(
        task_id="run_spark_batch",
        bash_command=r"""
docker exec spark bash -lc "
  /opt/spark/bin/spark-submit \
    --class com.myapp.logs.BatchAggJob \
    --master local[*] \
    /app/spark-batch/target/scala-2.12/logs-batch_2.12-0.1.0.jar {{ ds }}
"
""",
    )

    run_batch
