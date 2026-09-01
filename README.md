# uuidulid

[![Maven Central](https://img.shields.io/maven-central/v/io.github.bnymndev/uuidulid-core)](https://central.sonatype.com/artifact/io.github.bnymndev/uuidulid-core)

ULID and UUIDv7 for Java, plus the glue you need to use them as identifiers in a REST API:
Jackson serialisation, JPA/Hibernate mapping and Spring Boot auto-configuration.

The core has no dependencies and runs on Java 8.

```java
Ulid id = Ulid.random();   // 01K49B3X7K2QZ4T7S6M9GVFP8E
id.getInstant();           // 2026-09-01T18:30:12.345Z, the id carries its creation time
id.toUuid();               // same 128 bits as a java.util.UUID
```

## Modules

| Artifact | Java | Needs |
|---|---|---|
| `uuidulid-core` | 8 | nothing |
| `uuidulid-jackson` | 8 | Jackson 2.x |
| `uuidulid-jpa` | 8 | Jakarta Persistence 3.x |
| `uuidulid-jpa-javax` | 8 | `javax.persistence` 2.x |
| `uuidulid-hibernate` | 11 | Hibernate ORM 6.2+ |
| `uuidulid-spring-boot-starter` | 17 | Spring Boot 3.x |
| `uuidulid-spring-boot2-starter` | 8 | Spring Boot 2.x |
| `uuidulid-bom` | | pins all of the above |

Java 7 isn't supported. The API uses `java.time`, `Optional` and lambdas, and current JDKs can't
compile for Java 7 anymore.

## Getting it

All modules are on Maven Central under the group `io.github.bnymndev`. Import the BOM once and
leave the versions off the individual modules:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.bnymndev</groupId>
      <artifactId>uuidulid-bom</artifactId>
      <version>0.1.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>io.github.bnymndev</groupId>
    <artifactId>uuidulid-spring-boot-starter</artifactId>
  </dependency>
  <dependency>
    <groupId>io.github.bnymndev</groupId>
    <artifactId>uuidulid-hibernate</artifactId>
  </dependency>
</dependencies>
```

Or a single module with an explicit version:

```xml
<dependency>
  <groupId>io.github.bnymndev</groupId>
  <artifactId>uuidulid-core</artifactId>
  <version>0.1.0</version>
</dependency>
```

Gradle:

```kotlin
implementation("io.github.bnymndev:uuidulid-core:0.1.0")
```

For a Spring Boot 3 app the starter is enough; add `uuidulid-hibernate` if `Ulid` is used as an
entity id. Without Spring, `uuidulid-core` is all you need (plus `uuidulid-jackson` for JSON).

To try unreleased changes, build from source with `./mvnw install` and depend on the
`-SNAPSHOT` version from the POM, or use [JitPack](https://jitpack.io/#bnymnDev/uuidulid) with
`com.github.bnymnDev.uuidulid:<artifact>:main-SNAPSHOT`.

## ULID

```java
Ulid id = Ulid.random();                      // one-off, not monotonic

UlidFactory ids = UlidFactory.monotonic();    // keep one per application
Ulid next = ids.create();                     // strictly increasing, even within the same millisecond

Ulid parsed = Ulid.parse("01arz3ndektsv4rrffq69g5fav");   // case-insensitive, accepts I/L for 1 and O for 0
Optional<Ulid> maybe = Ulid.tryParse(input);
boolean ok = Ulid.isValid(input);

id.getTimestamp();          // epoch millis
id.getInstant();
id.getRandomness();         // 10 bytes

id.toString();              // 26 chars, Crockford base32
id.toBytes();               // 16 bytes, big-endian
id.toUuid();                // java.util.UUID with the same bits
Ulid.fromUuid(uuid);
Ulid.fromBytes(bytes);

// Bounds for a time range, e.g. WHERE id BETWEEN :from AND :to
Ulid from = Ulid.min(Instant.parse("2026-09-01T00:00:00Z"));
Ulid to   = Ulid.max(Instant.parse("2026-09-01T23:59:59.999Z"));

Ulid after = id.increment();   // exclusive bound for keyset pagination
```

`Ulid` is immutable, `Serializable` and `Comparable`. The comparison is unsigned and matches the
order of `toString()` and `toBytes()`, so sorting in Java, in the database and as strings all
agree.

A monotonic factory bumps the random part by one within the same millisecond, as the ULID spec
suggests. That makes consecutive ids from one millisecond guessable from each other. If ids are
public and must not be enumerable, use `UlidFactory.random()`; ordering across milliseconds
still holds.

## UUIDv7 and UUID helpers

```java
Uuid7Factory uuid7s = Uuid7Factory.monotonic();
UUID id = uuid7s.create();          // 019930f2-3c1e-7a4b-8f0d-2a6b3c4d5e6f

UUID quick = Uuids.v7();            // shared factory, not monotonic
UUID v4 = Uuids.v4();

Uuids.timestamp(id);                // epoch millis, works for versions 1, 6 and 7
Uuids.instant(id);
Uuids.isV7(id);

Uuids.parse("{017F22E2-79B0-7CC3-98C4-DC0C0C07398F}");   // dashes optional, braces and urn:uuid: accepted
Uuids.toBytes(uuid);
Uuids.fromBytes(bytes);

// UUID.compareTo is signed and doesn't sort like the string form. These do.
Uuids.compareUnsigned(a, b);
list.sort(Uuids.unsignedComparator());
```

UUIDv7 and ULID both start with a 48-bit millisecond timestamp, so `Ulid.fromUuid(uuidv7)` keeps
the creation time. UUIDv7 has 74 random bits instead of 80 but is a valid UUID, which matters
for native `uuid` columns and existing tooling.

### Clock and randomness

```java
UlidFactory factory = UlidFactory.builder()
        .clock(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC))
        .random(new SecureRandom())            // any java.util.Random
        .randomSource(generator::nextLong)     // or any LongSupplier, e.g. a Java 17 RandomGenerator
        .monotonic(true)
        .build();
```

The default is one `SecureRandom` per thread. Factories are thread-safe; the monotonic state
sits behind a `ReentrantLock`, so virtual threads don't get pinned.

## Spring Boot

With `uuidulid-spring-boot-starter` (Boot 3) or `uuidulid-spring-boot2-starter` (Boot 2) on the
classpath you get, without configuration:

- `UlidFactory` and `Uuid7Factory` beans (they also implement `Supplier`)
- conversion for `@PathVariable Ulid`, `@RequestParam Ulid` and `Ulid` properties; bad input is a 400
- the Jackson module, so `Ulid` fields are plain strings in request and response bodies

```java
@RestController
@RequestMapping("/api/notes")
class NoteController {

    private final NoteRepository notes;
    private final UlidFactory ulids;

    NoteController(NoteRepository notes, UlidFactory ulids) {
        this.notes = notes;
        this.ulids = ulids;
    }

    @PostMapping
    ResponseEntity<NoteResponse> create(@RequestBody NoteRequest req) {
        Note note = notes.save(new Note(ulids.create(), req.title(), req.body()));
        return ResponseEntity.created(URI.create("/api/notes/" + note.getId())).body(NoteResponse.from(note));
    }

    @GetMapping("/{id}")
    NoteResponse get(@PathVariable Ulid id) { ... }

    @GetMapping
    NotePage list(@RequestParam(required = false) Ulid after, @RequestParam(defaultValue = "20") int limit) { ... }
}

record NoteResponse(Ulid id, String title, Instant createdAt) { }   // "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV"
```

```yaml
uuidulid:
  monotonic: true   # false for non-enumerable ids
```

Define your own `UlidFactory`, `Uuid7Factory` or `UlidModule` bean and the auto-configuration
backs off.

## Persistence

### Hibernate 6

`uuidulid-hibernate` registers a Hibernate type for `Ulid` through `ServiceLoader`. `Ulid` then
works everywhere, including as `@Id`, with no mapping annotations:

```java
@Entity
public class Note {
    @Id
    private Ulid id;                                     // VARCHAR(26)
}

@Id @JdbcTypeCode(SqlTypes.BINARY) private Ulid id;      // BINARY(16), sorts the same
@Id @JdbcTypeCode(SqlTypes.UUID)   private Ulid id;      // native uuid column (PostgreSQL etc.)
```

Why a Hibernate type and not just a JPA converter: Hibernate doesn't apply `AttributeConverter`s
to `@Id` attributes. Without this module an `@Id Ulid` ends up as a serialised Java object in a
`VARBINARY` column. `UlidJavaTypeTest` covers all three column types against H2.

With Spring Data JPA, let the entity implement `Persistable<Ulid>` (see `Note` in the example).
The id is assigned by the application, and without `isNew()` every `save()` starts with a
`SELECT` to check whether the row exists.

Queries on the id work as expected because all column forms sort the same way:

```java
List<Note> findByIdGreaterThanOrderByIdAsc(Ulid after, Limit limit);      // keyset pagination
List<Note> findByIdBetweenOrderByIdAsc(Ulid from, Ulid to, Limit limit);   // time window via Ulid.min/max
```

### JPA converters

For other JPA providers, or for `Ulid` columns that aren't the id, `uuidulid-jpa` (Jakarta) and
`uuidulid-jpa-javax` (Spring Boot 2, Java EE 8) ship two `AttributeConverter`s:

```java
@Convert(converter = UlidStringConverter.class) private Ulid ref;   // CHAR(26), autoApply = true
@Convert(converter = UlidBinaryConverter.class) private Ulid ref;   // BINARY(16)
```

`java.util.UUID` (and therefore UUIDv7) needs no converter.

## Jackson without Spring

```java
ObjectMapper mapper = new ObjectMapper().registerModule(new UlidModule());
// or, via ServiceLoader:
ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
```

Values and map keys are supported. Invalid input throws `InvalidFormatException`.

## Example application

`uuidulid-example-api` is a Spring Boot 3 app with an in-memory H2 database that uses all of the above.

```bash
./mvnw -pl uuidulid-example-api spring-boot:run
```

| | |
|---|---|
| `POST /api/notes` | creates a note, returns 201 with `Location` and the ULID |
| `GET /api/notes/{id}` | 400 for a malformed id, 404 for an unknown one (RFC 9457 problem details) |
| `GET /api/notes?after=&limit=&from=&to=` | keyset pagination and time windows, both on the id |
| `DELETE /api/notes/{id}` | |
| `GET /api/ids/ulid?count=`, `/uuid7`, `/uuid4` | generates ids |
| `GET /api/ids/inspect/{value}` | takes a ULID or UUID apart: timestamp, version, the other form |

```bash
curl -s -X POST localhost:8080/api/notes -H 'Content-Type: application/json' \
     -d '{"title":"hello","body":"first note"}'
# {"id":"01K49B3X7K2QZ4T7S6M9GVFP8E","title":"hello","body":"first note","createdAt":"2026-09-01T18:30:12.345Z"}

curl -s localhost:8080/api/ids/inspect/01ARZ3NDEKTSV4RRFFQ69G5FAV
# {"kind":"ULID","ulid":"01ARZ3NDEKTSV4RRFFQ69G5FAV","uuid":"01563e3a-b5d3-d676-4c61-efb99302bd5b",
#  "uuidVersion":null,"timestamp":1469922850259,"instant":"2016-07-30T23:54:10.259Z"}
```

## Building

```bash
./mvnw verify                        # everything, needs JDK 17+
./mvnw verify -pl uuidulid-core      # the core alone builds and tests on JDK 8
```

The bytecode level of each module is enforced with `--release`. CI runs the full build on JDK 17
and 21 and the library modules on JDK 8 and 11.

The base32 codec is checked against an independent reference implementation, and the timestamp
extraction against the test vectors in RFC 9562 (versions 1, 6 and 7).

## License

MIT, see [LICENSE](LICENSE).
