#!/bin/bash
# Wrapper script to run Gradle with Java 17
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
exec ./gradlew "$@"
