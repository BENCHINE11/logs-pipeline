import json, os, random, time
from datetime import datetime, timezone
from kafka import KafkaProducer

BOOTSTRAP = os.getenv("KAFKA_BOOTSTRAP", "localhost:9092")
TOPIC = os.getenv("KAFKA_TOPIC", "logs_raw")
RATE = int(os.getenv("RATE", "150"))  # events/sec

HOSTS = ["api.myapp.com", "auth.myapp.com", "cdn.myapp.com"]
METHODS = ["GET", "POST", "PUT", "DELETE"]
PATHS = ["/api/v1/products", "/api/v1/orders", "/api/v1/login", "/api/v1/profile", "/health"]
UAS = ["Mozilla/5.0", "curl/8.0", "PostmanRuntime/7.39", "okhttp/4.10"]

def gen_event():
    status = random.choices([200, 201, 204, 301, 400, 401, 403, 404, 429, 500, 502, 503],
                            weights=[45,5,5,3,8,5,3,10,3,6,4,3])[0]
    return {
        "ts": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "host": random.choice(HOSTS),
        "method": random.choice(METHODS),
        "path": random.choice(PATHS),
        "status": status,
        "bytes": random.randint(100, 50000),
        "ip": f"41.251.{random.randint(0,255)}.{random.randint(1,254)}",
        "ua": random.choice(UAS),
        "rt_ms": random.randint(5, 800) if status < 500 else random.randint(100, 2000)
    }

producer = KafkaProducer(
    bootstrap_servers=BOOTSTRAP,
    value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    linger_ms=20,
    batch_size=32768
)

print(f"Producing to {TOPIC} @ {RATE} events/sec. Ctrl+C to stop.")
interval = 1.0 / RATE

try:
    while True:
        t0 = time.time()
        producer.send(TOPIC, gen_event())
        dt = time.time() - t0
        sleep = interval - dt
        if sleep > 0:
            time.sleep(sleep)
except KeyboardInterrupt:
    pass
finally:
    producer.flush()
    producer.close()
