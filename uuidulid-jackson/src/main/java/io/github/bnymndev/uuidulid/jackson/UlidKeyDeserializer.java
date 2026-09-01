package io.github.bnymndev.uuidulid.jackson;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import io.github.bnymndev.uuidulid.Ulid;

import java.io.IOException;
import java.util.Optional;

/** Reads a {@link Ulid} used as a {@link java.util.Map} key. */
public final class UlidKeyDeserializer extends KeyDeserializer {

    @Override
    public Ulid deserializeKey(String key, DeserializationContext context) throws IOException {
        Optional<Ulid> parsed = Ulid.tryParse(key.trim());
        if (parsed.isPresent()) {
            return parsed.get();
        }
        throw context.weirdKeyException(Ulid.class, key,
                "not a valid ULID (expected 26 Crockford Base32 characters)");
    }
}
