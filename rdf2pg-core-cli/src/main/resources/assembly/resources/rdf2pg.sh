#!/bin/bash

# This is the Bash Launcher.
#
# See rdf2pg.sh for details on how these files are used as templates for the 
# CLI-specific implementations.  
# 

if [ "$JAVA_TOOL_OPTIONS" == "" ]; then
  # So, let's set default JVM options here, unless you already have them from the outside
  # Note that this variable is part of standard Java (https://goo.gl/rrmXEX), so we don't need
  # to pass it to the java command below and possible further JVM invocations get it automatically too
  export JAVA_TOOL_OPTIONS="-Xmx4G"
fi

# We always work with universal text encoding.
JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dfile.encoding=UTF-8"

# Monitoring with jvisualvm/jconsole (end-user doesn't usually need this)
#JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS 
# -Dcom.sun.management.jmxremote.port=5010
# -Dcom.sun.management.jmxremote.authenticate=false
# -Dcom.sun.management.jmxremote.ssl=false"
       
# Used for invoking a command in debug mode (end user doesn't usually need this)
#JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Xdebug -Xnoagent"
#JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"

# You shouldn't need to change the rest
#
###

export WORK_DIR="$(pwd)"
if [ "$RDF2PG_HOME" == "" ]; then
	cd "$(dirname $0)"
	export RDF2PG_HOME="$(pwd)"
	cd "$WORK_DIR"
fi

# Additional .jar files or other CLASSPATH directories can be set with this.
# (see http://kevinboone.net/classpath.html for details)  
export CLASSPATH="$CLASSPATH:$RDF2PG_HOME:$RDF2PG_HOME/lib/*"

# See here for an explanation about ${1+"$@"} :
# http://stackoverflow.com/questions/743454/space-in-java-command-line-arguments 

java uk.ac.rothamsted.kg.rdf2pg.cli.Rdf2PGCli ${1+"$@"}

EXCODE=$?

# We assume stdout is for actual output, that might be pipelined to some other command, the rest (including logging)
# goes to stderr.
# 
echo -e "\nJava Finished, quitting the shell too.\n" >&2
exit $EXCODE
