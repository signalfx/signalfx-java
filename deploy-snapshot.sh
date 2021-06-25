#!/bin/bash

set -e

cat > snapshot-settings.xml <<EOF
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>${SONATYPE_USERNAME}</username>
      <password>${SONATYPE_PASSWORD}</password>
    </server>
  </servers>
</settings>
EOF
trap "rm snapshot-settings.xml" EXIT

if (grep SNAPSHOT pom.xml > /dev/null)
then
  ./mvnw -s snapshot-settings.xml clean deploy -P release-sign-artifacts
fi
