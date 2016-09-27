#!bin/bash

cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
CONF_DIR=$DEPLOY_DIR/conf
CLASSPATH=$DEPLOY_DIR/WEB-INF/classes;$DEPLOY_DIR/WEB-INF/lib/*
KAFKA_PRODUCE=$CONF_DIR/kafkaProduce.properties
CANGO_PORT=8080
CANGO_CONTEXT_PATH=
SERVER_NAME=CANGO

PIDS=`ps -f | grep java | grep "$SERVER_NAME" |awk '{print $2}'`
if [ -n "$PIDS" ]; then
    echo "ERROR: The $SERVER_NAME already started!"
    echo "PID: $PIDS"
    exit 1
fi

if [ -n "$CANGO_PORT" ]; then
    SERVER_PORT_COUNT=`netstat -tln | grep $SERVER_PORT | wc -l`
    if [ $SERVER_PORT_COUNT -gt 0 ]; then
        echo "ERROR: The $SERVER_NAME port $CANGO_PORT already used!"
        exit 1
    fi
fi

LOGS_DIR=""
if [ -n "$LOGS_FILE" ]; then
    LOGS_DIR=`dirname $LOGS_FILE`
else
    LOGS_DIR=$DEPLOY_DIR/logs
fi
if [ ! -d $LOGS_DIR ]; then
    mkdir $LOGS_DIR
fi
STDOUT_FILE=$LOGS_DIR/stdout.log

JAVA_OPTS=" -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true "
JAVA_DEBUG_OPTS=""
if [ "$1" = "debug" ]; then
    JAVA_DEBUG_OPTS=" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n "
fi

JAVA_MEM_OPTS=""
BITS=`java -version 2>&1 | grep -i 64-bit`
if [ -n "$BITS" ]; then
    JAVA_MEM_OPTS=" -server -Xmx2g -Xms1g -Xmn256m -XX:PermSize=128m -Xss256k -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70   -Dremoting.bind_by_host=false  -Djava.awt.headless=true"
else
   JAVA_MEM_OPTS=" -server -Xms1g -Xmx2g -XX:PermSize=128m -XX:SurvivorRatio=2 -XX:+UseParallelGC  -Dremoting.bind_by_host=false  -Djava.awt.headless=true"
fi

CANGO_OPTS="-DkafkaProduce=$KAFKA_PRODUCE "
if [ $CANGO_PORT -gt 0 ]; then
  CANGO_OPTS=$CANGO_OPTS" -Dcango.port=$CANGO_PORT "
fi

if [ -n "$CANGO_CONTEXT_PATH" ]; then
  CANGO_OPTS=$CANGO_OPTS" -Dcango.contextPath=$CANGO_CONTEXT_PATH "
fi

if [ -n "$DEPLOY_DIR" ]; then
  CANGO_OPTS=$CANGO_OPTS" -Dcango.docBase=$DEPLOY_DIR "
fi

echo -e "Starting the $SERVER_NAME ...\c"
nohup java $JAVA_OPTS $JAVA_MEM_OPTS $JAVA_DEBUG_OPTS $CANGO_OPTS -classpath $CLASSPATH com.bella.cango.server.startUp.TomcatBootstrap >> $STDOUT_FILE 2>&1 &

echo "OK!"
PIDS=`ps -f | grep java | grep "$DEPLOY_DIR" | awk '{print $2}'`
echo "PID: $PIDS"
echo "STDOUT: $STDOUT_FILE"