FROM wurstmeister/kafka:2.11-1.1.1

WORKDIR /opt

COPY docker/run.sh /opt/run.sh
COPY docker/connect-distributed.properties /opt/kafka/config/connect-distributed.properties

RUN mkdir -p /opt/connectors
COPY target/scala-2.13/kafka-connect-http-sink.jar /opt/connectors/

CMD ["/opt/run.sh"]