# Builder
FROM openjdk:8-jdk-slim as builder

# Env variables
ENV SCALA_VERSION 2.12.8
ENV SBT_VERSION 1.2.8

WORKDIR /build

# Install sbt
RUN \
    set -eux; \
    apt-get update && \
    apt-get install -y --no-install-recommends curl git && \
    curl -fsL https://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz | tar xfz - -C /root/ && \
    echo >> /root/.bashrc && \
    echo "export PATH=~/scala-$SCALA_VERSION/bin:$PATH" >> /root/.bashrc && \
    curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
    dpkg -i sbt-$SBT_VERSION.deb && \
    rm sbt-$SBT_VERSION.deb && \
    apt-get update && \
    apt-get install sbt && \
    \
    apt-get remove -y --auto-remove \
        curl \
        ; \
    rm -rf /var/lib/apt/lists/*;

COPY . /build

RUN sbt -mem 2048 stage

# Runtime
FROM openjdk:8-jre-slim

# Create a non root user for runtime
RUN addgroup jvm && \
    adduser --system --disabled-login --shell /bin/false --home=/app jvm && \
    adduser jvm jvm && chown jvm:jvm -R /app

WORKDIR /app

# Copy pre-build api to runtime container
COPY --from=builder /build/target/universal/stage ./

RUN chmod 755 -R /app/bin

USER jvm

# Create keystore directory
RUN mkdir /app/keystore

EXPOSE 9000

# Default play application entrypoint
ENTRYPOINT [ "bin/ton-api", "-Dpidfile.path=/dev/null", "-Dfile.encoding=UTF8" ]
