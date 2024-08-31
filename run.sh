#!/usr/bin/env bash

JAVA_OPTS="-Xmx256m \
 -Xss256k \
 -XX:+UseShenandoahGC \
 "
 java $JAVA_OPTS -jar build/libs/1brc-all-1.0-SNAPSHOT.jar "$@"