#!/bin/bash

# This file and tdb2pg.sh are templated in rdf2pg-core-cli.
# They're poured on a specific PG command line interface with more proper names (eg rdf2neo.sh, rdf2graphml.sh).
#
# Moreover, the variables ${rdf2pg.xxx} are defined in the CLI POMs to define sensible values for these files
# (via Maven interpolation). Hence, they're not Bash variables.
#
export WORK_DIR="$(pwd)"
export MYNAME=`basename $0` # My name is changed in every specific package

if  [ "$1" == '-h' ] || [ "$1" == '--help' ] || [ $# -lt 3 ] || ! ( [ "$1" == '-c' ] || [ "$1" == '--config' ] ); then
				cat <<EOT
	
	
	*** ${rdf2pg.cli.title} ***
	
	$MYNAME -c|--config <bean config file> <RDF-FILE>...
	
	Loads the files into the TDB triple store set by RDF2PG_TDB (uses a default in /tmp if not set),
	then invokes tdb2pg.sh passing this TDB and the -c option.
		
	Requires JENA_HOME to be set.	
	
EOT
  exit 1
fi

if [ "$RDF2PG_HOME" == "" ]; then
	cd "$(dirname $0)"
	export RDF2PG_HOME="$(pwd)"
	cd "$WORK_DIR"
fi

if [ "$RDF2PG_TDB" == "" ]; then
	export RDF2PG_TDB=/tmp/rdf2pg_tdb
	echo "Generating new TDB at '$RDF2PG_TDB'"
  rm -Rf "$RDF2PG_TDB"
  mkdir "$RDF2PG_TDB"
fi

if [ "$JENA_HOME" == '' ]; then
	echo -e "\n\n  Please set JENA_HOME to the path of the Jena binaries, which includes bin/ utilities\n"
	exit 1
fi

shift
config_path="$1"
shift


# See here for an explanation about ${1+"$@"} :
# http://stackoverflow.com/questions/743454/space-in-java-command-line-arguments 

echo -e "\n\n  Invoking tdbloader\n"
"$JENA_HOME/bin/tdbloader" --loc="$RDF2PG_TDB" ${1+"$@"}

echo -e "\n\n  Invoking ${rdf2pg.cli.tdb2pg}.sh"
"$RDF2PG_HOME/${rdf2pg.cli.tdb2pg}.sh" -c "$config_path" "$RDF2PG_TDB"

excode=$?
echo -e "\n\n $MYNAME finished"
exit $excode
