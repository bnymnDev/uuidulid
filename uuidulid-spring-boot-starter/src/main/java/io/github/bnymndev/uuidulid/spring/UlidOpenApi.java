package io.github.bnymndev.uuidulid.spring;

import io.github.bnymndev.uuidulid.Ulid;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.utils.SpringDocUtils;

/**
 * OpenAPI description of {@link Ulid} for springdoc.
 *
 * <p>The auto-configuration calls {@link #register()} when springdoc is present. Applications
 * that configure springdoc without Spring Boot can call it themselves once at startup.
 */
public final class UlidOpenApi {

    /** Canonical text form: a leading digit 0-7 followed by 25 Crockford base32 characters. */
    public static final String PATTERN = "^[0-7][0-9A-HJKMNP-TV-Z]{25}$";

    /** The example from the ULID specification. */
    public static final String EXAMPLE = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    private UlidOpenApi() {
    }

    /**
     * Builds the schema used for every {@code Ulid} property and parameter.
     *
     * @return a string schema with pattern, length and example
     */
    public static Schema<String> schema() {
        return new StringSchema()
                .pattern(PATTERN)
                .minLength(Ulid.LENGTH)
                .maxLength(Ulid.LENGTH)
                .example(EXAMPLE)
                .description("ULID: 26 Crockford base32 characters, sortable by creation time. "
                        + "Input is case-insensitive.");
    }

    /** Registers {@link #schema()} as the replacement for {@code Ulid} in springdoc's model converters. */
    public static void register() {
        SpringDocUtils.getConfig().replaceWithSchema(Ulid.class, schema());
    }
}
