#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
IMAGES_TAR="${SCRIPT_DIR}/health-system-images.tar"

echo "Loading images from ${IMAGES_TAR}..."
docker load -i "$IMAGES_TAR"

# Create network if not exists
NETWORK_NAME="health_net"
if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  docker network create "$NETWORK_NAME"
fi

# Stop and remove existing containers
docker rm -f health-frontend >/dev/null 2>&1 || true
docker rm -f health-backend >/dev/null 2>&1 || true
docker rm -f health-mysql >/dev/null 2>&1 || true

echo "Starting MySQL..."
docker run -d \
  --name health-mysql \
  --network "$NETWORK_NAME" \
  -e MYSQL_ROOT_PASSWORD=12345678 \
  -e MYSQL_DATABASE=health_manage_db \
  -e TZ=Asia/Shanghai \
  -p 3306:3306 \
  health-manage-mysql:latest \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci

echo "Waiting for MySQL to be ready..."
for i in {1..40}; do
  if docker exec health-mysql mysql -uroot -p12345678 -e "SELECT 1" >/dev/null 2>&1; then
    break
  fi
  sleep 3
done

echo "Starting Backend..."
docker run -d \
  --name health-backend \
  --network "$NETWORK_NAME" \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL='jdbc:mysql://health-mysql:3306/health_manage_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=12345678 \
  -e SPRING_HTTP_ENCODING_ENABLED=true \
  -e SPRING_HTTP_ENCODING_FORCE=true \
  -e SPRING_HTTP_ENCODING_CHARSET=UTF-8 \
  -e JAVA_TOOL_OPTIONS='-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8' \
  health-manage-backend:latest

echo "Starting Frontend..."
docker run -d \
  --name health-frontend \
  --network "$NETWORK_NAME" \
  -p 5173:80 \
  health-manage-frontend:latest

echo "Started containers:"
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'

echo "Access URLs:"
echo "  Frontend:  http://localhost:5173/"
echo "  Backend:   http://localhost:8080/"
