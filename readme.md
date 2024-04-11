# nima-games

# Development

## Dependencies

* java - https://adoptium.net/installation/
* clojure - `brew install clojure/tools/clojure`
* docker - https://docs.docker.com/desktop/install/mac-install/
* poly - `brew install polyfy/polylith/poly`
* clj-kondo - `brew install borkdude/brew/clj-kondo`
* zprint - `brew install --cask zprint`

## Getting things working with your IDE

* see https://cljdoc.org/d/polylith/clj-poly/0.2.19/doc/development
* some notes for Cursive
    * select the Aliases `dev, test, build` in the Clojure Deps tool window to resolve all dependencies you have to work with
    * Go to `Settings→Languages & Frameworks→Clojure→Project Specific Options and check "Resolve over whole project"` to make things resolve correctly across different subprojects with our root `deps.edn` setup

## Running locally

* start a clojure repl (ie `clj -A:dev:test:build`)
* the dev system will start on its own via loading `development/src/user.clj`
* go to http://localhost:9000/api-docs/ to see endpoints and send requests from your browser

## Linting and formatting

You can create a pre commit hook to lint and format files before they are committed

* `touch .git/hooks/pre-commit`
* add contents
```bash
#!/usr/bin/env bash
if ! (./bin/zprint-diff.sh)
then
  exit 1
fi
./bin/kondo-diff.sh
exit $?
```
* `chmod +x .git/hooks/pre-commit`

You can force a commit through if you want with the `--no-verify` flag
