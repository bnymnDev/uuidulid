package io.github.bnymndev.uuidulid.spring;

import io.github.bnymndev.uuidulid.Ulid;
import org.springframework.core.convert.converter.Converter;

/**
 * Lets Spring bind request parameters, path variables and configuration properties to
 * {@link Ulid}. Malformed input throws {@link IllegalArgumentException}, which Spring MVC and
 * WebFlux report as {@code 400 Bad Request}.
 */
public class StringToUlidConverter implements Converter<String, Ulid> {

    @Override
    public Ulid convert(String source) {
        String trimmed = source.trim();
        return trimmed.isEmpty() ? null : Ulid.parse(trimmed);
    }
}
