#!/bin/bash
mvn clean package
cp target/MicrOS-1.0-SNAPSHOT-jar-with-dependencies.jar .
java -jar MicrOS-1.0-SNAPSHOT-jar-with-dependencies.jar