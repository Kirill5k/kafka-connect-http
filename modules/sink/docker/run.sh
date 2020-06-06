#!/bin/bash

while ! nc -z kafka 9092; do
  sleep 3
done

echo "Kafka is now listening. Starting Connect"

sleep 10

export CLASSPATH="/usr/share/java/*"
/opt/kafka/bin/connect-distributed.sh /opt/kafka/config/connect-distributed.properties