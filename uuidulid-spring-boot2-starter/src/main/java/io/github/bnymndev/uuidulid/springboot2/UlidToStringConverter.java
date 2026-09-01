package io.github.bnymndev.uuidulid.springboot2;

import io.github.bnymndev.uuidulid.Ulid;
import org.springframework.core.convert.converter.Converter;

/** Formats a {@link Ulid} as its canonical 26-character string wherever Spring converts to text. */
public class UlidToStringConverter implements Converter<Ulid, String> {

    @Override
    public String convert(Ulid source) {
        return source.toString();
    }
}
