#!/bin/bash
file_name=$1
shift

#export JAVA_HOME=jdk1.8

java -cp jar/bitmovin-api-sdk-example-1.0-SNAPSHOT-jar-with-dependencies.jar $file_name "$@"
