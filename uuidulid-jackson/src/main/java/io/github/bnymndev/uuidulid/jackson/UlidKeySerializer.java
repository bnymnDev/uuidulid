package io.github.bnymndev.uuidulid.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.github.bnymndev.uuidulid.Ulid;

import java.io.IOException;

/** Writes a {@link Ulid} used as a {@link java.util.Map} key. */
public final class UlidKeySerializer extends StdSerializer<Ulid> {

    private static final long serialVersionUID = 1L;

    public UlidKeySerializer() {
        super(Ulid.class);
    }

    @Override
    public void serialize(Ulid value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeFieldName(value.toString());
    }
}
