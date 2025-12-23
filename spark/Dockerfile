FROM apache/spark:3.5.1

USER 0

# Prérequis
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl ca-certificates gnupg apt-transport-https openjdk-17-jdk \
  && rm -rf /var/lib/apt/lists/*

# Installer sbt via repo officiel (clé stable)
RUN set -eux; \
  curl -fsSL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x99E82A75642AC823" \
  | gpg --dearmor > /usr/share/keyrings/sbt-archive-keyring.gpg; \
  echo "deb [signed-by=/usr/share/keyrings/sbt-archive-keyring.gpg] https://repo.scala-sbt.org/scalasbt/debian all main" \
  > /etc/apt/sources.list.d/sbt.list; \
  echo "deb [signed-by=/usr/share/keyrings/sbt-archive-keyring.gpg] https://repo.scala-sbt.org/scalasbt/debian /" \
  >> /etc/apt/sources.list.d/sbt.list; \
  apt-get update; \
  apt-get install -y sbt; \
  rm -rf /var/lib/apt/lists/*


WORKDIR /app
