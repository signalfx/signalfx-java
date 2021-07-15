#!/bin/bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd ${SCRIPT_DIR}

print_usage() {
  echo "Usage: ./$(basename $0) [snapshot|release]"
}

if [[ $# < 1 ]]
then
  print_usage
  exit 1
fi

case "$1" in
  snapshot)
    if (! grep SNAPSHOT pom.xml > /dev/null)
    then
      echo "Non-SNAPSHOT release found, skipping"
      exit 0
    fi
    ;;

  release)
    if (grep SNAPSHOT pom.xml > /dev/null)
    then
      echo "You can't release a SNAPSHOT artifact!"
      exit 1
    fi
    ;;

  *)
    print_usage
    exit  1
    ;;
esac

echo ">>> Setting GnuPG configuration ..."
mkdir -p ~/.gnupg
chmod 700 ~/.gnupg
cat > ~/.gnupg/gpg.conf <<EOF
no-tty
pinentry-mode loopback
EOF

echo ">>> Importing secret key ..."
gpg --batch --allow-secret-key-import --import "${GPG_SECRET_KEY}"

echo ">>> Building settings.xml ..."
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
      <properties>
        <gpg.passphrase>${GPG_PASSWORD}</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
</settings>
EOF
trap "rm release-settings.xml" EXIT INT KILL STOP TERM

echo ">>> Running maven ..."
./mvnw -s release-settings.xml clean deploy -P release-sign-artifacts,gpg
