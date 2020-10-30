FROM confluentinc/cp-kafka-connect-base:5.2.4

RUN apt-get update \
  && apt-get install -y --force-yes jq \
  && rm -rf /var/lib/apt/lists/*

# Note: the following environment variables must be passed when running a container:
# - CONNECT_BOOTSTRAP_SERVERS
# - CONNECT_GROUP_ID
# - CONNECT_OFFSET_STORAGE_TOPIC
# - CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR
# - CONNECT_CONFIG_STORAGE_TOPIC
# - CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR
# - CONNECT_STATUS_STORAGE_TOPIC
# - CONNECT_STATUS_STORAGE_REPLICATION_FACTOR
# - CONNECT_OFFSET_FLUSH_INTERVAL_MS
# - CONNECT_REST_PORT
# - CONNECT_REST_ADVERTISED_PORT

ENV CONNECT_KEY_CONVERTER=org.apache.kafka.connect.converters.ByteArrayConverter
ENV CONNECT_VALUE_CONVERTER=org.apache.kafka.connect.converters.ByteArrayConverter
ENV CONNECT_KEY_CONVERTER_SCHEMAS_ENABLE=false
ENV CONNECT_VALUE_CONVERTER_SCHEMAS_ENABLE=false

ENV CONNECT_INTERNAL_KEY_CONVERTER=org.apache.kafka.connect.json.JsonConverter
ENV CONNECT_INTERNAL_VALUE_CONVERTER=org.apache.kafka.connect.json.JsonConverter
ENV CONNECT_INTERNAL_KEY_CONVERTER_SCHEMAS_ENABLE=false
ENV CONNECT_INTERNAL_VALUE_CONVERTER_SCHEMAS_ENABLE=false

ENV CONNECT_REST_HOST_NAME=0.0.0.0

ENV CONNECT_PLUGIN_PATH=/opt/connectors

RUN mkdir -p /opt/connectors
COPY sink/target/scala-2.13/kafka-connect-http-sink.jar /opt/connectors/

COPY entrypoint.sh /opt/
COPY healthcheck.sh /opt/

CMD /opt/entrypoint.sh
