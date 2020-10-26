#!/bin/bash

# This is the Bash Launcher.
# 

# These are passed to the JVM. they're appended, so that you can predefine it from the shell
OPTS="$OPTS -Xms2G -Xmx4G"

# We always work with universal text encoding.
OPTS="$OPTS -Dfile.encoding=UTF-8"

# Monitoring with jvisualvm/jconsole (end-user doesn't usually need this)
#OPTS="$OPTS 
# -Dcom.sun.management.jmxremote.port=5010
# -Dcom.sun.management.jmxremote.authenticate=false
# -Dcom.sun.management.jmxremote.ssl=false"
       
# Used for invoking a command in debug mode (end user doesn't usually need this)
#OPTS="$OPTS -Xdebug -Xnoagent"
#OPTS="$OPTS -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"

# You shouldn't need to change the rest
#
###

export WORK_DIR="$(pwd)"
if [ "$RDF2NEO_HOME" == "" ]; then
	cd "$(dirname $0)"
	export RDF2NEO_HOME="$(pwd)"
	cd "$WORK_DIR"
fi

# Additional .jar files or other CLASSPATH directories can be set with this.
# (see http://kevinboone.net/classpath.html for details)  
export CLASSPATH="$CLASSPATH:$RDF2NEO_HOME:$RDF2NEO_HOME/lib/*"

# See here for an explanation about ${1+"$@"} :
# http://stackoverflow.com/questions/743454/space-in-java-command-line-arguments 

java \
	$OPTS uk.ac.rothamsted.rdf.neo4j.Rdf2NeoCli ${1+"$@"}

EXCODE=$?

# We assume stdout is for actual output, that might be pipelined to some other command, the rest (including logging)
# goes to stderr.
# 
echo Java Finished. Quitting the Shell Too. >&2
echo >&2
exit $EXCODE
