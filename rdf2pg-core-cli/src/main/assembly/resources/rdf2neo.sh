#!/bin/bash
export WORK_DIR="$(pwd)"

if  [ "$1" == '-h' ] || [ "$1" == '--help' ] || [ $# -lt 3 ] || ! ( [ "$1" == '-c' ] || [ "$1" == '--config' ] ); then
				cat <<EOT
	
	
	*** Rdf2Neo, the RDF-to-Neo4j converter ***
	
	$(basename $0) -c|--config <bean config file> <RDF-FILE>...
	
	Loads the files into the TDB triple store set by RDF2NEO_TDB (uses a default in /tmp if not set),
	then invokes tdb2rdf.sh passing this TDB and the -c option.
		
	Requires JENA_HOME to be set.	
	
EOT
  exit 1
fi

if [ "$RDF2NEO_HOME" == "" ]; then
	cd "$(dirname $0)"
	export RDF2NEO_HOME="$(pwd)"
	cd "$WORK_DIR"
fi

if [ "$RDF2NEO_TDB" == "" ]; then
	export RDF2NEO_TDB=/tmp/rdf2neo_tdb
	echo "Generating new TDB at '$RDF2NEO_TDB'"
  rm -Rf "$RDF2NEO_TDB"
  mkdir "$RDF2NEO_TDB"
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
"$JENA_HOME/bin/tdbloader" --loc="$RDF2NEO_TDB" ${1+"$@"}

echo -e "\n\n  Invoking tdb2rdf.sh"
"$RDF2NEO_HOME/tdb2neo.sh" -c "$config_path" "$RDF2NEO_TDB"

excode=$?
echo -e "\n\n  rdf2neo.sh finished"
exit $excode
