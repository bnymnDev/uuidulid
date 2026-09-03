package io.github.bnymndev.uuidulid.example.postgres;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Example: UUIDv7 primary keys inside PostgreSQL, ULIDs on the REST API.
 *
 * <p>Both identifiers are the same 128 bits, so the translation at the API boundary is a pure
 * re-encoding with no lookup table. See {@link PublicId}.
 */
@SpringBootApplication
public class PostgresExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostgresExampleApplication.class, args);
    }
}
