#!/usr/bin/env bash
set -euo pipefail

NACOS_ADDR="${NACOS_ADDR:-127.0.0.1:8848}"
NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
NACOS_PASSWORD="${NACOS_PASSWORD:-nacos}"
NACOS_GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
CONFIG_DIR="${CONFIG_DIR:-deploy/nacos/configs}"

for _ in {1..60}; do
  if curl -fsS --max-time 2 "http://${NACOS_ADDR}/nacos/v1/console/health/readiness" >/dev/null; then
    break
  fi
  sleep 2
done

token="$(
  curl -sS -X POST "http://${NACOS_ADDR}/nacos/v1/auth/login" \
    -d "username=${NACOS_USERNAME}" \
    -d "password=${NACOS_PASSWORD}" |
    sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'
)"

if [[ -z "${token}" ]]; then
  echo "Failed to login to Nacos at ${NACOS_ADDR}" >&2
  exit 1
fi

while IFS= read -r -d '' file; do
  data_id="$(basename "${file}")"
  echo "Publishing ${data_id} to ${NACOS_GROUP}"
  curl -sS -X POST "http://${NACOS_ADDR}/nacos/v1/cs/configs" \
    -d "accessToken=${token}" \
    -d "dataId=${data_id}" \
    -d "group=${NACOS_GROUP}" \
    -d "type=yaml" \
    --data-urlencode "content@${file}" >/dev/null
done < <(find "${CONFIG_DIR}" -maxdepth 1 -type f -print0 | sort -z)

echo "Nacos configs published."
