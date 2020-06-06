#!/bin/bash

while ! nc -z kafka 9092; do
  sleep 3
done

echo "kafka is now listening. Starting Connect"

export CLASSPATH="/usr/share/java/*"
/opt/kafka/bin/connect-distributed.sh /opt/kafka/config/connect-distributed.properties