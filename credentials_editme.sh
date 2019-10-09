#!/bin/sh

#export PATH=`pwd`/spark-2.4.4-bin-hadoop3.2/bin:$PATH
#export PATH=`pwd`/sbt/bin:$PATH
#export PATH=`pwd`/:$PATH
#export SPARK_SCALA_VERSION=2.11

# OpenStack credential
export OS_AUTH_URL=https://auth.cloud.ovh.net/v2.0/
export OS_IDENTITY_API_VERSION=2
export OS_TENANT_ID=XXXX
export OS_TENANT_NAME="XXXX"
export OS_USERNAME="XXXX"
export OS_PASSWORD="XXXX"
export OS_REGION_NAME="GRA5"

# OVH Metrics
export METRICS_WRITE_TOKEN="XXX"
export METRICS_READ_TOKEN="XXX"

# OVH Logs
export LDP_WSS="wss://XXX.logs.ovh.com/tail/?tk=XXXX"
export LDP_WRITE_TOKEN="XXXX"
