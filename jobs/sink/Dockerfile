FROM debian:stretch-slim

RUN apt-get update -y && \
    apt-get install -y curl jq

COPY ./run.sh /opt/run.sh
COPY job-config-templates /opt/job-config-templates
WORKDIR /opt

ENTRYPOINT ["/opt/run.sh"]
