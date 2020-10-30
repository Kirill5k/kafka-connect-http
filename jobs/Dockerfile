FROM debian:stretch-slim

RUN apt-get update -y && \
    apt-get install -y curl jq gettext && \
    rm -rf /var/lib/apt/lists/*

COPY run.sh /opt/run.sh
COPY ./submit-job.sh /opt/submit-job.sh
COPY job-config-templates /opt/job-config-templates
WORKDIR /opt

ENTRYPOINT ["/opt/run.sh"]
