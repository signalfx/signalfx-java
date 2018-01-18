#!/bin/bash
grep SNAPSHOT pom.xml > /dev/null && echo "You shouldn't be releasing a SNAPSHOT artifact!" && exit 1
mvn -DperformRelease=true clean deploy -P release-sign-artifacts
