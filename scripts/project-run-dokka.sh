#!/bin/bash

set -ex
echo "For $1 ..."
cd $BASEDIR/$1
./gradlew clean dokka
