# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Changed
- Validation tests pin `jboss-logging` to a Java 8 compatible version so the JDK 8 CI job runs
  again (the Spring Boot BOM manages a version compiled for Java 11).

### Added
- Contributing guide, security policy, issue and pull request templates, Dependabot configuration.

## [0.2.0] - 2026-09-03

### Added
- `uuidulid-validation` (Java 8): `@ValidUlid` Bean Validation constraint for `String` fields.
- `uuidulid-spring-boot-starter`: `Ulid` is described as a 26-character string in OpenAPI
  documents when springdoc is on the classpath.
- `uuidulid-example-postgres`: Spring Boot 3 example with a native `uuid` primary key generated as
  UUIDv7, ULID-typed DTOs and path variables, a `PublicId` translator that rejects ULIDs which
  don't encode a UUIDv7, keyset pagination and time-window queries on the primary key alone.
  Tests run against an embedded PostgreSQL 17.

## [0.1.0] - 2026-09-01

First release on Maven Central.

### Added
- `uuidulid-core` (Java 8, no dependencies): `Ulid` value type, monotonic and random
  `UlidFactory`, RFC 9562 `Uuid7Factory`, `Uuids` helpers.
- `uuidulid-jackson`: Jackson module for values and map keys.
- `uuidulid-jpa` / `uuidulid-jpa-javax`: `AttributeConverter`s for `CHAR(26)` and `BINARY(16)`.
- `uuidulid-hibernate` (Java 11): Hibernate 6 type that also works for `@Id` attributes.
- `uuidulid-spring-boot-starter` (Java 17) / `uuidulid-spring-boot2-starter` (Java 8): factory
  beans, web converters, Jackson auto-configuration.
- `uuidulid-bom` and a runnable example REST API (`uuidulid-example-api`).

[Unreleased]: https://github.com/bnymnDev/uuidulid/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/bnymnDev/uuidulid/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/bnymnDev/uuidulid/releases/tag/v0.1.0
