#!/usr/bin/env bash
################################################################################
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
# limitations under the License.
################################################################################

# Start a Flink service as a console application. Must be stopped with Ctrl-C
# or with SIGTERM by kill or the controlling process.

# 启动一个 Flink 服务作为控制台应用程序。必须使用 Ctrl-C 停止 或者使用 信号 kill 或 控制进程
USAGE="Usage: flink-console.sh (jobmanager|taskmanager|zookeeper) [args]"

SERVICE=$1
ARGS=("${@:2}") # get remaining arguments as array

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

. "$bin"/config.sh

case $SERVICE in
    (jobmanager)
        CLASS_TO_RUN=org.apache.flink.runtime.jobmanager.JobManager
    ;;

    (taskmanager)
        CLASS_TO_RUN=org.apache.flink.runtime.taskmanager.TaskManager
    ;;

    (taskexecutor)
        CLASS_TO_RUN=org.apache.flink.runtime.taskexecutor.TaskManagerRunner
    ;;

    (zookeeper)
        CLASS_TO_RUN=org.apache.flink.runtime.zookeeper.FlinkZooKeeperQuorumPeer
    ;;

    (standalonesession)
        CLASS_TO_RUN=org.apache.flink.runtime.entrypoint.StandaloneSessionClusterEntrypoint
    ;;

    (*)
        echo "Unknown service '${SERVICE}'. $USAGE."
        exit 1
    ;;
esac

FLINK_TM_CLASSPATH=`constructFlinkClassPath`

log_setting=("-Dlog4j.configuration=file:${FLINK_CONF_DIR}/log4j-console.properties" "-Dlogback.configurationFile=file:${FLINK_CONF_DIR}/logback-console.xml")

JAVA_VERSION=$(${JAVA_RUN} -version 2>&1 | sed 's/.*version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q')

# Only set JVM 8 arguments if we have correctly extracted the version
if [[ ${JAVA_VERSION} =~ ${IS_NUMBER} ]]; then
    if [ "$JAVA_VERSION" -lt 18 ]; then
        JVM_ARGS="$JVM_ARGS -XX:MaxPermSize=256m"
    fi
fi

echo "Starting $SERVICE as a console application on host $HOSTNAME."
exec $JAVA_RUN $JVM_ARGS ${FLINK_ENV_JAVA_OPTS} "${log_setting[@]}" -classpath "`manglePathList "$FLINK_TM_CLASSPATH:$INTERNAL_HADOOP_CLASSPATHS"`" ${CLASS_TO_RUN} "${ARGS[@]}"
