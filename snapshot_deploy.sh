#!/bin/bash
grep SNAPSHOT pom.xml > /dev/null && mvn -DperformRelease=true clean deploy
