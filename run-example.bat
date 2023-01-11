set file_name=%1
shift

java -cp target/bitmovin-api-sdk-example-1.0-SNAPSHOT-jar-with-dependencies.jar %file_name% %*
