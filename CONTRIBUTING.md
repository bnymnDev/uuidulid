# Contributing

Thanks for taking the time to contribute. Small, focused pull requests are the easiest to review.

## Building

The build uses the Maven wrapper, so only a JDK is needed.

```bash
./mvnw verify                                  # full reactor, needs JDK 17+
./mvnw verify -pl uuidulid-core                # a single module; the core builds on JDK 8
./mvnw verify -pl uuidulid-hibernate -am       # a module and everything it depends on
./mvnw -DskipTests package                     # compile and package only
```

CI (`.github/workflows/build.yml`) runs the full build on JDK 17 and 21, and the library modules on
their real minimum runtimes (JDK 8 and 11). A change is ready when all of those pass.

## Running tests

Tests use JUnit 5 and AssertJ. `uuidulid-example-postgres` starts an embedded PostgreSQL that is
downloaded from Maven Central on first run; no Docker is needed.

```bash
./mvnw test -pl uuidulid-core -Dtest=UlidTest   # one test class
```

## Conventions

- Each module has a bytecode baseline (`maven.compiler.release`), see the table in the README.
  Code in a Java 8 module must not use newer language features or APIs.
- `uuidulid-core` has no runtime dependencies. Keep it that way.
- Public API is `final` and immutable where possible, and every public type and method has Javadoc.
- New behaviour comes with tests. Encoding changes must keep the base32 codec and RFC 9562
  timestamp test vectors green.
- Don't change published behaviour (string form, byte order, comparison order) without a
  discussion in an issue first.
- Follow the existing code style: four-space indentation, no wildcard imports, no trailing
  whitespace. The compiler runs with `-Xlint:all`; fix warnings rather than suppressing them.

## Pull requests

1. Open an issue first for anything beyond a small fix, so the approach can be agreed on.
2. Branch from `main`, keep the history clean and write commit messages in the imperative
   ("Add X", "Fix Y").
3. Update the README and `CHANGELOG.md` (under "Unreleased") when user-visible behaviour changes.
4. Make sure `./mvnw verify` passes before opening the PR.
