#!/bin/bash
mvn -Drelease-sign-artifacts=true clean deploy -P release
