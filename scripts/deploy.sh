#!/bin/bash

DIR_WORK=/speak-for-the-trees-backend
DIR_PERSIST=${DIR_WORK}/common/src/main/resources/properties
DIR_JAR=${DIR_WORK}/service/target

# Check for updated database username
if [[ -z "${SFTT_DB_USERNAME}" ]]; then
    echo "ERROR: environment variable not set for database username"
    exit 1
fi

# Check for updated database password
if [[ -z "${SFTT_DB_PASSWORD}" ]]; then
    echo "ERROR: environment variable not set for database password"
    exit 1
fi

# Check for updated database url
if [[ -z "${SFTT_DB_URL}" ]]; then
    echo "ERROR: environment variable not set for database url"
    exit 1
fi

# Check for existing db.properties file
if [[ ! -f "${DIR_PERSIST}/db.properties.example" ]]; then
    echo "ERROR: \"db.properties.example\" file not found"
    exit 1
fi

# Update the "db.properties" file
cd ${DIR_PERSIST} || exit 1
cp db.properties.example input.properties

awk -F" = " -v updatedVal="= $SFTT_DB_USERNAME" '/database.username =/{$2=updatedVal}1' input.properties > user.properties
awk -F" = " -v updatedVal="= $SFTT_DB_PASSWORD" '/database.password =/{$2=updatedVal}1' user.properties > pass.properties
awk -F" = " -v updatedVal="= $SFTT_DB_URL" '/database.url =/{$2=updatedVal}1' pass.properties > db.properties
rm user.properties
rm pass.properties
rm input.properties

mv emailer.properties.example emailer.properties
mv expiration.properties.example expiration.properties
mv jwt.properties.example jwt.properties

echo "SUCCESS: updated database credentials"

# Perform maven install/package
cd ${DIR_WORK} || exit 1
mvn -T 2C install
mvn -T 2C package

# Execute the jar file
java -jar ${DIR_JAR}/service-1.0-SNAPSHOT-jar-with-dependencies.jar
