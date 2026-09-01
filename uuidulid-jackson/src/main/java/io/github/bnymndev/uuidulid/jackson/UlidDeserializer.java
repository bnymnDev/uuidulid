package io.github.bnymndev.uuidulid.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.github.bnymndev.uuidulid.Ulid;

import java.io.IOException;
import java.util.Optional;

/**
 * Reads a {@link Ulid} from its 26-character string form.
 *
 * <p>Malformed input is reported through
 * {@link DeserializationContext#weirdStringException}, which yields a
 * {@link com.fasterxml.jackson.databind.exc.InvalidFormatException}; web frameworks such as
 * Spring MVC translate that into a {@code 400 Bad Request}.
 */
public final class UlidDeserializer extends StdDeserializer<Ulid> {

    private static final long serialVersionUID = 1L;

    public UlidDeserializer() {
        super(Ulid.class);
    }

    @Override
    public Ulid deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_STRING) {
            String text = parser.getText().trim();
            Optional<Ulid> parsed = Ulid.tryParse(text);
            if (parsed.isPresent()) {
                return parsed.get();
            }
            throw context.weirdStringException(text, Ulid.class,
                    "not a valid ULID (expected 26 Crockford Base32 characters)");
        }
        if (token == JsonToken.VALUE_EMBEDDED_OBJECT) {
            Object embedded = parser.getEmbeddedObject();
            if (embedded instanceof byte[] && ((byte[]) embedded).length == Ulid.BYTES) {
                return Ulid.fromBytes((byte[]) embedded);
            }
            if (embedded instanceof Ulid) {
                return (Ulid) embedded;
            }
        }
        return (Ulid) context.handleUnexpectedToken(Ulid.class, parser);
    }
}
