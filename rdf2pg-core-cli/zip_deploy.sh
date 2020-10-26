#!/bin/sh

# Deploys the command line binary. This doesn't do much, but it might be useful with CI systems.
# 

MYDIR=$(dirname "$0")

cd "$MYDIR"
version="$(mvn help:evaluate -Dexpression=project.version | grep -v '\[')"
artifact="$(mvn help:evaluate -Dexpression=project.artifactId | grep -v '\[')"

base_name=$artifact"_$version"

cd target

target="$1"

if [ "$target" == "" ]; then
	target=.
fi;

echo
echo 
echo "_______________ Deploying the Command Line Binary ($version) to $target _________________"

# We need to remove old versions and unused libs
rm -Rf "$target/$base_name/lib"

yes A| unzip $base_name.zip -d "$target"
cp -f $base_name.zip "$target"
chmod -R ug=rwX,o=rX "$target/$base_name" "$target/$base_name.zip"

echo ______________________________________________________________________________
echo
echo
echo
