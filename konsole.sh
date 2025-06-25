#!/bin/bash
mvn clean package
cp target/MicrOS-1.0-SNAPSHOT-jar-with-dependencies.jar ./MicrOS.jar
java -jar MicrOS.jar --app org.finite.Konsole