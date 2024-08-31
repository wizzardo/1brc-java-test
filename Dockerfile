#FROM bellsoft/liberica-openjdk-alpine:21
FROM bellsoft/liberica-openjdk-debian:21

WORKDIR /tmp/app
COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
RUN ./gradlew --no-daemon

COPY build.gradle .
RUN ./gradlew --no-daemon -Dorg.gradle.jvmargs="-Xmx2g -Xms2g" resolveDependencies

COPY src src
RUN ./gradlew --no-daemon -Dorg.gradle.jvmargs="-Xmx2g -Xms2g" fatJar

COPY weather_stations.csv .

COPY ./run.sh /
ENTRYPOINT ["/run.sh"]