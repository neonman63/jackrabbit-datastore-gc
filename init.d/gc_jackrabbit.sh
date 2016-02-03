#!/bin/sh
### BEGIN INIT INFO
# Provides:          jackrabbit
# Short-Description: Start/stop GC Jackrabbit JCR server.
#
# Default-Start:    2 3 4 5
# Default-Stop:     0 1 6
# Required-Start:
# Required-Stop:
#
# Author:           Maxim Popov <mpopov@lanit.ru><gentoozoid@gmail.com>
#
### END INIT INFO

BASEDIR=/opt/gc_jackrabbit_mfcresource
GC_JACKRABBIT_JAR=$BASEDIR/GCJackRabbit-0.0.1-SNAPSHOT-jar-with-dependencies.jar
MEMORY="-XX:MaxPermSize=128m \
        -Xmx512M \
        -Xms128M"
PIDFILE=$BASEDIR/gc_jackrabbit.pid
LOGFILE=$BASEDIR/gc_console.log

# Uncomment to debug the script
#set -x

do_start() {
    if [ ! -f $PIDFILE ]; then
        cd $BASEDIR
		    nohup java $MEMORY -jar $GC_JACKRABBIT_JAR >> $LOGFILE 2>&1 & echo $! > $PIDFILE
        echo "GC Jackrabbit started"
    else
        echo "GC Jackrabbit is already running"
    fi
}

do_stop() {
    if [ -f $PIDFILE ]; then
        kill $(cat $PIDFILE)
        rm $PIDFILE
        echo "GC Jackrabbit stopped"
    else
        echo "GC Jackrabbit is not running"
    fi
    exit 3
}

do_status() {
    if [ -f $PIDFILE ]; then
          echo "GC Jackrabbit is running [ pid = " $(cat $PIDFILE) "]"
    else
        echo "GC Jackrabbit is not running"
        exit 3
    fi
}

case "$1" in
  start)
        do_start
        ;;
  stop)
        do_stop
        ;;
    status)
        do_status
        ;;
  *)
        echo "Usage: $SCRIPTNAME {start|stop|status}" >&2
        exit 3
    ;;
esac
