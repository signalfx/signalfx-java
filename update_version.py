#!/usr/bin/env python3

# Copyright (C) 2017 SignalFx, Inc. All rights reserved.

# This script is used to update the versions of all the artifacts, the
# User-Agent version of the library, and the documented version in the README
# file, all at once, as part of the release process.

import logging
import os
import re
import subprocess
from subprocess import PIPE
import sys

logging.basicConfig(stream=sys.stdout, level=logging.INFO)
logger = logging.getLogger(os.path.basename(__file__))

def match_all(v):
    return True

def no_snapshots(v):
    return 'SNAPSHOT' not in v

FILE_REPLACES = {
    'signalfx-connection/src/main/java/com/signalfx/connection/AbstractHttpReceiverConnection.java': [
        (match_all, re.compile(r'public static final String VERSION_NUMBER = "(.*?)"'),
         'public static final String VERSION_NUMBER = "%s"')
    ],
    'legacy-usage.md': [
        (no_snapshots, re.compile(r'<version>([^<]+)</version>'),
         '<version>%s</version>'),
        (no_snapshots, re.compile(r'libraryDependencies \+= "com.signalfx.public" % "signalfx-codahale" % "(.*?)"'),
         'libraryDependencies += "com.signalfx.public" %% "signalfx-codahale" %% "%s"'),
        (no_snapshots, re.compile(r'libraryDependencies \+= "com.signalfx.public" % "signalfx-yammer" % "(.*?)"'),
         'libraryDependencies += "com.signalfx.public" %% "signalfx-yammer" %% "%s"'),
    ],
}


def execute(cmd, expected_code=None, stdin=None, background=False):
    logger.info('Executing in %s: %s', os.getcwd(), ' '.join(cmd))
    proc = subprocess.Popen(cmd, stdin=PIPE, stdout=PIPE, stderr=PIPE)
    if background:
        return ('', '', 0)  # In background
    stdout, stderr = proc.communicate(stdin)
    logger.debug('Result (%s, %s, %d)', stdout, stderr, proc.returncode)
    ret = (stdout, stderr, proc.returncode)
    if expected_code is not None and expected_code != ret[2]:
        raise Exception('Unable to execute command %s, result: %s', ret)
    return ret


def update_pom_files(version):
    base_dir = os.getcwd()
    logger.info('Updating POM files to version %s...', version)
    cmd = ['mvn', 'versions:set', 'versions:update-child-modules',
           '-DnewVersion=%s' % version]
    (stdout, _, code) = execute(cmd, expected_code=0)
    os.chdir('signalfx-java-examples')
    (stdout, _, code) = execute(cmd, expected_code=0)
    os.chdir(base_dir)


def perform_file_replacements(version):
    for file_name, repls in FILE_REPLACES.items():
        logger.info('Updating %d version number location%s in %s...',
                    len(repls), 's' if len(repls) != 1 else '', file_name)
        for repl in repls:
            if not repl[0](version):
                continue
            logger.debug('%s -> %s', repl[1], repl[2])
            file_name = os.path.join(os.getcwd(), file_name)
            with open(file_name, 'r') as f:
                contents = f.read()
            contents = repl[1].sub(repl[2] % version, contents)
            with open(file_name, 'w') as f:
                f.write(contents)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        sys.stderr.write(f"usage: {sys.argv[0]} <version>\n")
        sys.exit(1)

    version = sys.argv[1]
    version = re.sub(r'^v', '', version)
    update_pom_files(version)
    perform_file_replacements(version)
