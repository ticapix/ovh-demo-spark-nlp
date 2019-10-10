#!/bin/sh

set -e

for var in SPARK_VERSION SBT_VERSION; do
    [ -z $( eval "echo \$$var" ) ] && (echo "Variable $var is undefined."; exit 1)
done

ls sbt-${SBT_VERSION}.tgz > /dev/null || wget --continue https://piccolo.link/sbt-${SBT_VERSION}.tgz
ls sbt > /dev/null || tar zxfv sbt-${SBT_VERSION}.tgz

# fetch source
ls spark-${SPARK_VERSION}.tgz > /dev/null || wget --continue http://mirrors.ircam.fr/pub/apache/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}.tgz
# extract
ls spark-${SPARK_VERSION} > /dev/null || tar zxfv spark-${SPARK_VERSION}.tgz
# compile
ls spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-standalone.tgz > /dev/null || \
(cd spark-${SPARK_VERSION} && ./dev/make-distribution.sh --name standalone --tgz -Pscala-2.11 -DskipTests)
# 'fetch' compiled version
ls spark-${SPARK_VERSION}-bin-standalone.tgz > /dev/null || ln -sf spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-standalone.tgz .
# extract
ls spark-${SPARK_VERSION}-bin-standalone > /dev/null || tar zxfv spark-${SPARK_VERSION}-bin-standalone.tgz
# create symlink for future access
ls spark > /dev/null || ln -sf spark-${SPARK_VERSION}-bin-standalone spark
