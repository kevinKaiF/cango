#!bin/bash

MAIN_CLASS=com.bella.cango.server.startUp.TomcatBootstrap
SERVER_NAME=CANGO

echo "Stopping $SERVER_NAME"

PIDs=`jps -l | grep $MAIN_CLASS | awk '{print $1}'`

if [ -n "$PIDs" ]; then
  for PID in $PIDs; do
      kill $PID
      echo "kill $PID"
  done
fi

for i in {1..10}; do
  PIDs=`jps -l | grep $MAIN_CLASS | awk '{print $1}'`
  if [ ! -n "$PIDs" ]; then
    echo "Stop $SERVER_NAME successfully"
    break
  fi
  sleep 5
done

PIDs=`jps -l | grep $MAIN_CLASS | awk '{print $1}'`
if [ -n "$PIDs" ]; then
  for PID in $PIDs; do
      kill -9 $PID
      echo "kill -9 $PID"
  done
fi