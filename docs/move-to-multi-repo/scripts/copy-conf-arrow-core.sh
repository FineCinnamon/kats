#!/bin/bash

set -e
export BASEDIR=$(pwd)

echo "arrow-core ..."
cd $BASEDIR/arrow-core
cp $BASEDIR/d-arrow-module/arrow-core-repository/README.md .
cp -r $BASEDIR/d-arrow-module/arrow-core-repository/*gradle* .
sed -i "s/d-arrow/arrow/g" gradle.properties
for module in arrow-*; do
    cp $BASEDIR/d-arrow-module/arrow-core-repository/$module/build.gradle $module/
    cp $BASEDIR/d-arrow-module/arrow-core-repository/$module/gradle.properties $module/
done
mv arrow-meta/models arrow-meta/arrow-meta-test-models
cp $BASEDIR/d-arrow-module/arrow-core-repository/arrow-meta/arrow-meta-test-models/build.gradle arrow-meta/arrow-meta-test-models/
rm arrow-meta/arrow-meta-test-models/gradle.properties
cp $BASEDIR/d-arrow-module/.gitignore .

mkdir -p .github/workflows/
cp $BASEDIR/d-arrow-module/.github/workflows/*arrow-core* .github/workflows/
cp $BASEDIR/d-arrow-module/.github/workflows/check* .github/workflows/
sed -i "s/d-arrow-module/arrow-core/g" .github/workflows/*
sed -i "s/d-arrow/arrow/g" .github/workflows/*
sed -i "s/sh arrow-core-repository/sh/g" .github/workflows/*

git co -b new-conf
git add .
git ci -m "Configuration for the new multi-repo organization"
git push upstream new-conf

#diff -r . ../d-arrow-module/arrow-core-repository/
