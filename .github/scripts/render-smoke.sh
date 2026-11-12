#!/usr/bin/env bash
set -euo pipefail
cleanup() {
  local result=$?
  if [ "$result" -ne 0 ]; then docker logs product-cloud-smoke || true; fi
  docker rm -f product-cloud-smoke product-cloud-postgres >/dev/null 2>&1 || true
  docker network rm product-cloud-ci >/dev/null 2>&1 || true
}
trap cleanup EXIT
docker network create product-cloud-ci
docker run -d --name product-cloud-postgres --network product-cloud-ci \
  -e POSTGRES_DB=product_cloud -e POSTGRES_USER=product_cloud \
  -e POSTGRES_PASSWORD=ci-only-product-cloud postgres:17
for attempt in $(seq 1 60); do
  if docker exec product-cloud-postgres pg_isready -U product_cloud -d product_cloud >/dev/null 2>&1; then break; fi
  sleep 2
done
docker run -d --name product-cloud-smoke --network product-cloud-ci \
  -p 127.0.0.1:10000:10000 \
  -e SPRING_PROFILES_ACTIVE=render -e PORT=10000 \
  -e DB_HOST=product-cloud-postgres -e DB_PORT=5432 -e DB_NAME=product_cloud \
  -e DB_USERNAME=product_cloud -e DB_PASSWORD=ci-only-product-cloud \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://product-cloud-postgres:5432/product_cloud \
  -e MAIL_ENABLED=false product-cloud:verify
for attempt in $(seq 1 60); do
  if curl --fail --silent --output /dev/null http://127.0.0.1:10000/; then
    echo "Docker: Render profile, PostgreSQL migrations, port 10000 and HTTP 200 verified"
    exit 0
  fi
  sleep 2
done
exit 1
