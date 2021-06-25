#!/bin/bash

set -e

if (grep SNAPSHOT pom.xml > /dev/null)
then
  echo "You can't release a SNAPSHOT artifact!"
  exit 1
fi

echo "${GPG_PUBLIC_KEY}" > pub.gpg
echo "${GPG_SECRET_KEY}" > sec.gpg
trap "rm pub.gpg sec.gpg" EXIT

gpg --import pub.gpg
gpg --allow-secret-key-import --import sec.gpg

cat > release-settings.xml <<EOF
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>${SONATYPE_USERNAME}</username>
      <password>${SONATYPE_PASSWORD}</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>gpg</id>
      <activation>
        <activeByDefault>false</activeByDefault>
      </activation>
      <properties>
        <gpg.passphrase>${GPG_PASSWORD}</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
</settings>
EOF
trap "rm release-settings.xml" EXIT

./mvnw -s release-settings.xml clean deploy -P release-sign-artifacts
