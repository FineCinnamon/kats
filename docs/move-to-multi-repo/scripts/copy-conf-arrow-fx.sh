#!/bin/bash

set -e
export BASEDIR=$(pwd)

echo "arrow-fx ..."
cd $BASEDIR/arrow-fx
cp $BASEDIR/d-arrow-module/arrow-fx-repository/README.md .
cp -r $BASEDIR/d-arrow-module/arrow-fx-repository/*gradle* .
sed -i "s/d-arrow/arrow/g" gradle.properties
for module in arrow-*; do
    cp $BASEDIR/d-arrow-module/arrow-fx-repository/$module/build.gradle $module/
    cp $BASEDIR/d-arrow-module/arrow-fx-repository/$module/gradle.properties $module/
done
cp $BASEDIR/d-arrow-module/arrow-fx-repository/arrow-benchmarks-fx/arrow-kio-benchmarks/build.gradle arrow-benchmarks-fx/arrow-kio-benchmarks/
cp $BASEDIR/d-arrow-module/arrow-fx-repository/arrow-benchmarks-fx/arrow-scala-benchmarks/build.gradle arrow-benchmarks-fx/arrow-scala-benchmarks/
rm arrow-benchmarks-fx/arrow-kio-benchmarks/gradle.properties
rm arrow-benchmarks-fx/arrow-scala-benchmarks/gradle.properties
cp $BASEDIR/d-arrow-module/.gitignore .

mkdir -p .github/workflows/
cp $BASEDIR/d-arrow-module/.github/workflows/*arrow-fx* .github/workflows/
cp $BASEDIR/d-arrow-module/.github/workflows/check* .github/workflows/
sed -i "s/d-arrow-module/arrow-core/g" .github/workflows/*
sed -i "s/d-arrow/arrow/g" .github/workflows/*
sed -i "s/sh arrow-fx-repository/sh/g" .github/workflows/*

git co -b new-conf
git add .
git ci -m "Configuration for the new multi-repo organization"
git push upstream new-conf

#diff -r . ../d-arrow-module/arrow-fx-repository/
