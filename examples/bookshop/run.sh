#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
# В репозитории уже есть Maven Wrapper: отдельная установка Maven не нужна.
if [ -f ../../mvnw ]; then
  exec bash ../../mvnw -f "$PWD/pom.xml" compile exec:java
else
  exec mvn compile exec:java
fi
