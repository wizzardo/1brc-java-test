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

ENV JAVA_OPTS "-Xmx256m \
 -Xss256k \
 -XX:+UseShenandoahGC \
 "

CMD ["sh", "-c", "java ${JAVA_OPTS} -jar build/libs/1brc-all-1.0-SNAPSHOT.jar"]