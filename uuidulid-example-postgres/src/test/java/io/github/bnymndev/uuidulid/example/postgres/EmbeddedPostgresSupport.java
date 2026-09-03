package io.github.bnymndev.uuidulid.example.postgres;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * One real PostgreSQL per test JVM, downloaded as a Maven artifact and started from a temp
 * directory. No Docker, no local installation. Shut down with the JVM.
 */
final class EmbeddedPostgresSupport {

    static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start embedded PostgreSQL", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                POSTGRES.close();
            } catch (IOException ignored) {
                // best effort at JVM exit
            }
        }));
    }

    private EmbeddedPostgresSupport() {
    }

    /** Points Spring's data source at the embedded instance. */
    static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }
}
