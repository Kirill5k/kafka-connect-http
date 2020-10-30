#!/bin/bash

set -e

# You must provide the config file name you want to submit to Connect as the first argument
config_file="$1"

connector_name="$(jq -r .name $config_file)"

# Note: envsubst will replace any ${..} placeholders with the corresponding environment variable
connector_config="$(jq .config $config_file | envsubst)"

echo $connector_config | \
  curl -s \
    -X PUT \
    -H "Content-Type: application/json" \
    -d @- \
    "${KAFKA_CONNECT_URL}/connectors/${connector_name}/config" | \
  jq .
