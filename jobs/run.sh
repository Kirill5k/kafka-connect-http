#!/bin/bash

: ${KAFKA_CONNECT_URL?"Mandatory environment variable KAFKA_CONNECT_URL is not set"}
: ${KAFKA_BOOTSTRAP_SERVERS?"Mandatory environment variable KAFKA_BOOTSTRAP_SERVERS is not set"}
: ${SCHEMA_REGISTRY_URL?"Mandatory environment variable SCHEMA_REGISTRY_URL is not set"}

echo "Kafka connect's URL is $KAFKA_CONNECT_URL."

echo "This is what a curl to Kafka Connect looks like"
curl $KAFKA_CONNECT_URL

while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' $KAFKA_CONNECT_URL)" != "200" ]]; do
  echo "Waiting for Kafka Connect to be ready..."
  sleep 3
done

echo "Kafka Connect is ready."

for filename in /opt/connector-config-templates/*.json; do
  echo "Submitting job ${filename}"
  ./submit-job.sh "${filename}"
  echo "Finished submitting job ${filename}"
done
