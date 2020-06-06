#!/bin/bash

while [[ "$(curl -s -o /dev/null -w ''%http_code'' connect-http-sink:8083)" != "200" ]]; do sleep 3; done

echo "connect-http-sink is ready"

rm -rf /opt/job-configs
mkdir -p /opt/job-configs
cp /opt/job-config-templates/* /opt/job-configs

for config_file_name in /opt/job-configs/*.json; do
  echo "submitting job ${config_file_name}"
  curl -X POST -H "Content-Type: application/json" --data "@${config_file_name}" http://connect-http-sink:8083/connectors | jq .
  echo "job ${config_file_name} has been submitted"
done
