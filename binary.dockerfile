FROM bellsoft/liberica-openjdk-debian:21  as builder

WORKDIR /tmp/app
COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
RUN ./gradlew --no-daemon

COPY build.gradle .
RUN ./gradlew --no-daemon -Dorg.gradle.jvmargs="-Xmx2g -Xms2g" resolveDependencies

COPY src src
RUN ./gradlew --no-daemon -Dorg.gradle.jvmargs="-Xmx2g -Xms2g" fatJar

FROM debian:12-slim

RUN apt update \
    && apt install -y \
    wget \
    curl \
    build-essential \
    xz-utils \
    unzip \
    libz-dev \
    zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN wget -O jdk.tar.gz https://download.oracle.com/graalvm/22/latest/graalvm-jdk-22_linux-aarch64_bin.tar.gz \
    && mkdir /app/jdk \
    && tar xzvf jdk.tar.gz -C /app/jdk --strip-components=1 \
    && rm jdk.tar.gz


COPY --from=builder /tmp/app/build/libs/1brc-all-1.0-SNAPSHOT.jar app.jar
COPY weather_stations.csv .


RUN mkdir -p META-INF/native-image \
  && /app/jdk/bin/java  --add-opens java.base/java.lang=ALL-UNNAMED -agentlib:native-image-agent=config-output-dir=META-INF/native-image -jar app.jar \
  && /app/jdk/bin/java  --add-opens java.base/java.lang=ALL-UNNAMED -agentlib:native-image-agent=config-merge-dir=META-INF/native-image  -jar app.jar weather_stations.csv

RUN /app/jdk/bin/native-image \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    -H:ConfigurationFileDirectories=META-INF/native-image \
    --no-fallback  \
    -march=native  \
    -O3  \
    -jar app.jar app

COPY ./run-binary.sh /
ENTRYPOINT ["/run-binary.sh"]
