#!/usr/bin/env python

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

logging.basicConfig(stream=sys.stdout, level=logging.DEBUG)
logger = logging.getLogger(__name__)

FILE_REPLACES = {
    'signalfx-java/src/main/java/com/signalfx/connection/AbstractHttpReceiverConnection.java': [
        (re.compile(r'public static final String VERSION_NUMBER = "(.*?)"'),
         'public static final String VERSION_NUMBER = "%s"')
    ],
    'README.md': [
        (re.compile(r'<version>([^<]+)</version>'),
         '<version>%s</version>'),
        (re.compile(r'libraryDependencies \+= "com.signalfx.public" % "signalfx-codahale" % "(.*?)"'),
         'libraryDependencies += "com.signalfx.public" %% "signalfx-codahale" %% "%s"'),
        (re.compile(r'libraryDependencies \+= "com.signalfx.public" % "signalfx-yammer" % "(.*?)"'),
         'libraryDependencies += "com.signalfx.public" %% "signalfx-yammer" %% "%s"'),
    ],
}


def execute(cmd, expected_code=None, stdin=None, background=False):
    logger.debug('Executing %s', cmd)
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
    logging.info('Updating POM files to version %s...', version)
    cmd = ['mvn', 'versions:set', 'versions:update-child-modules',
           '-DnewVersion=%s' % version]
    (stdout, _, code) = execute(cmd, expected_code=0)
    os.chdir('signalfx-java-examples')
    (stdout, _, code) = execute(cmd, expected_code=0)
    os.chdir(base_dir)


def perform_file_replacements(version):
    for file_name, repls in FILE_REPLACES.items():
        logging.info('Updating %d version number location%s in %s...',
                     len(repls), 's' if len(repls) != 1 else '', file_name)
        for repl in repls:
            logging.debug('%s -> %s', *repl)
            file_name = os.path.join(os.getcwd(), file_name)
            with open(file_name, 'r') as f:
                contents = f.read()
            contents = repl[0].sub(repl[1] % version, contents)
            with open(file_name, 'w') as f:
                f.write(contents)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        sys.stderr.write('usage: %s <version>%n', sys.argv[0])
        sys.exit(1)

    version = sys.argv[1]
    update_pom_files(version)
    perform_file_replacements(version)
