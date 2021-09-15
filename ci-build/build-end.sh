[[ "$GIT_BRANCH" == 'master' ]] || return

echo -e "\n\n\tGenerating New Downloads Page\n"

cd /tmp
rm -Rf rdf2pg.wiki
git clone https://github.com/Rothamsted/rdf2pg.wiki.git
cd rdf2pg.wiki

down_script="$MYDIR/ci-build/mk-downloads-page.sh"
$down_script >Downloads.md

git diff --exit-code || (  
  echo -e "\n\n\tCommitting Wiki Changes\n"
  git commit -a -m "[CI] Updating Downloads Page."
  git push --set-upstream origin master # credentials are already set at this point
)

cd "$MYDIR"
