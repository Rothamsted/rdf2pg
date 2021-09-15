echo -e "\n\n\tGenerating New Downloads Page\n"

cd /tmp
rm -Rf rdf2pg.wiki
git clone https://github.com/Rothamsted/rdf2pg.wiki.git
cd rdf2pg.wiki

down_script="$MYDIR/ci-build/mk-download-page.sh"
$down_script >Downloads.md

git diff --exit-code || (  
  echo -e "\n\n\tCommitting Wiki Changes\n"
  git commit -a -m "[CI] Updating Downloads Page."
  git push --set-upstream origin master # credentials are already set at this point
)

cd "$MYDIR"


# If there aren't differences in the download page, we might as well stop here, but let's not 
# over-complicate things, this is a rare case (eg, during tests against the CI pipeline)
#

# Jenkins will do internal stuff, such as updating download links and deploying
# on our servers.
#
echo -e "\n\n\tTriggering RRes deployment\n"
  
job='ondex_rres_deployment'
curl --user "$KNET_JENKINS_USER:$KNET_JENKINS_TOKEN" -X POST -o - --fail \
     "https://knetminer.org/build/job/$job/build"
