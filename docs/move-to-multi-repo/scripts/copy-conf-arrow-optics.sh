#!/bin/bash

set -e
export BASEDIR=$(pwd)

echo "arrow-optics ..."
cd $BASEDIR/arrow-optics
cp $BASEDIR/d-arrow-module/arrow-optics-repository/README.md .
cp -r $BASEDIR/d-arrow-module/arrow-optics-repository/*gradle* .
sed -i "s/d-arrow/arrow/g" gradle.properties
for module in arrow-*; do
    cp $BASEDIR/d-arrow-module/arrow-optics-repository/$module/build.gradle $module/
    cp $BASEDIR/d-arrow-module/arrow-optics-repository/$module/gradle.properties $module/
done
cp $BASEDIR/d-arrow-module/.gitignore .

mkdir -p .github/workflows/
cp $BASEDIR/d-arrow-module/.github/workflows/*arrow-optics* .github/workflows/
cp $BASEDIR/d-arrow-module/.github/workflows/check* .github/workflows/
sed -i "s/d-arrow-module/arrow-core/g" .github/workflows/*
sed -i "s/d-arrow/arrow/g" .github/workflows/*
sed -i "s/sh arrow-optics-repository/sh/g" .github/workflows/*

git co -b new-conf
git add .
git ci -m "Configuration for the new multi-repo organization"
git push upstream new-conf

#diff -r . ../d-arrow-module/arrow-optics-repository/
