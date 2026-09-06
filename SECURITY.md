# Security Policy

## Supported versions

Only the latest release on Maven Central receives fixes. Please upgrade before reporting an issue
that may already be resolved.

## Reporting a vulnerability

Please do not open a public issue for security problems.

Use GitHub's private vulnerability reporting instead:
https://github.com/bnymnDev/uuidulid/security/advisories/new

Include the affected module and version, a description of the problem and, if possible, steps or
a small snippet to reproduce it. You will get a response within a few days, and a fix or an
advisory once the issue has been confirmed.

## Scope

`uuidulid-core` generates identifiers and does not touch the network or the file system. Note that
the monotonic factories deliberately produce guessable consecutive ids within one millisecond, as
described in the README; use `UlidFactory.random()` where ids must not be enumerable. That is
documented behaviour, not a vulnerability.
