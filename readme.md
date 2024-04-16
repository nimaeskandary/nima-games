# nima-games

Going through this [LWJGL Vulkan book](https://github.com/lwjglgamedev/vulkanbook) and porting the lessons to clojure in `components/vulkan-tutorial`

# Polylith cheatsheet

Polylith in a nutshell is an opinionated tool to help manage a monorepo. The guardrails in place help the monorepo share
libraries of common code across multiple build artifacts, e.g. webservers, lambdas, cli tools. At a high level,
individual libs of common code are called `components` by the tool. Each component exposes an `interface` package. Outside
of each component, e.g. from another component, you may only access what is inside the `interface` package of another
component, everything else is private. The root `deps.edn` is so your local development environment and REPL can
resolve all the code in the repo. In `projects/...` will be `deps.edn` files for individual build targets, and will only
include components required to build that project.


Running `poly check` will give you warnings like your project depends on something else in the monorepo that it is not using,
or errors like you are trying to use something from a namespace that is not in an interface package

### testing

* `poly test`

This can include options like what bases, components, or projects you want to run tests for, with nothing will run tests
for bases and components your projects in `projects/` depend on

If you use Intellij with the Cursive plugin, you can also execute tests like normal in your REPL

### Libraries

* `poly libs` - see project dependencies
* `poly libs :outdated` - see outdated dependencies
* `poly libs :update` - update dependencies

### Creating a component

building block that encapsulates a specific domain or part of the system

* `poly create component name:user`
* add component to `<root>/deps.edn`
```clojure
{:aliases  {:dev {:extra-paths ["development/src"]
                  ;; to extra dev deps
                  :extra-deps {poly/user {:local/root "components/user"}}}}
            ;; to extra test paths
            :test {:extra-paths ["components/user/test"]}}
```

### creating a base

building block that exposes a public API to the outside world, e.g., external systems and users

* `poly create base name:web`
* add base to `<root>/deps.edn`
```clojure
:aliases  {:dev {:extra-paths ["development/src"]
                  ;; to extra dev deps
                  :extra-deps {poly/web {:local/root "bases/cli"}}}
            ;; to extra test paths
            :test {:extra-paths ["bases/cli/test"]}}
```

### creating a project

used to build a deployable artifact

* `poly create project name:backend`
* add project to `<root>/workspace.edn`
```clojure
{:projects {"backend" {:alias "backend"}}}
```
* add components and bases to `<root>/projects/backend/deps.edn` e.g.
```clojure
{:deps {poly/user {:local/root "../../components/user"}
        poly/web  {:local/root "../../bases/cli"}}}
```
