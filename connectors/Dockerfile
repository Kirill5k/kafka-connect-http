FROM confluentinc/cp-kafka-connect-base:7.3.0

USER root
RUN mkdir -p /opt/connectors
COPY sink/target/scala-2.13/kafka-connect-http-sink.jar /opt/connectors/
