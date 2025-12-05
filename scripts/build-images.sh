#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPTS_DIR="${ROOT_DIR}/scripts"
TAR_PATH="${SCRIPTS_DIR}/health-system-images.tar"

echo "Building backend image..."
docker build -t health-manage-backend:latest "${ROOT_DIR}/health-manage-backend"

echo "Building frontend image..."
docker build -t health-manage-frontend:latest "${ROOT_DIR}/health-manage-frontend"

echo "Building mysql image (with init.sql)..."
docker build -t health-manage-mysql:latest -f "${ROOT_DIR}/docker/mysql/Dockerfile" "${ROOT_DIR}"

echo "Saving images to ${TAR_PATH}..."
mkdir -p "${SCRIPTS_DIR}"
docker save health-manage-backend:latest health-manage-frontend:latest health-manage-mysql:latest -o "${TAR_PATH}"

echo "Done. Generated files:"
ls -lh "${TAR_PATH}" "${SCRIPTS_DIR}/run.sh" || true
echo "Use: sh ${SCRIPTS_DIR}/run.sh"

