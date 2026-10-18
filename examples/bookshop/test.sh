#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [ -f ../../mvnw ]; then exec bash ../../mvnw -f "$PWD/pom.xml" test; else exec mvn test; fi
