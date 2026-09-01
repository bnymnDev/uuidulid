package io.github.bnymndev.uuidulid.jackson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.github.bnymndev.uuidulid.Ulid;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class UlidModuleTest {

    private static final Ulid ULID = Ulid.parse("01ARZ3NDEKTSV4RRFFQ69G5FAV");

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new UlidModule());

    static final class Note {
        private Ulid id;
        private String title;

        Note() {
        }

        Note(Ulid id, String title) {
            this.id = id;
            this.title = title;
        }

        public Ulid getId() {
            return id;
        }

        public void setId(Ulid id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Note && Objects.equals(id, ((Note) o).id) && Objects.equals(title, ((Note) o).title);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, title);
        }
    }

    @Test
    void serialisesAsCanonicalString() throws Exception {
        assertThat(mapper.writeValueAsString(ULID)).isEqualTo("\"01ARZ3NDEKTSV4RRFFQ69G5FAV\"");
        assertThat(mapper.writeValueAsString(new Note(ULID, "hello")))
                .isEqualTo("{\"id\":\"01ARZ3NDEKTSV4RRFFQ69G5FAV\",\"title\":\"hello\"}");
    }

    @Test
    void deserialisesFromString() throws Exception {
        assertThat(mapper.readValue("\"01ARZ3NDEKTSV4RRFFQ69G5FAV\"", Ulid.class)).isEqualTo(ULID);
        assertThat(mapper.readValue("{\"id\":\"01arz3ndektsv4rrffq69g5fav\",\"title\":\"x\"}", Note.class))
                .isEqualTo(new Note(ULID, "x"));
        assertThat(mapper.readValue("[\"01ARZ3NDEKTSV4RRFFQ69G5FAV\"]", new TypeReference<List<Ulid>>() { }))
                .containsExactly(ULID);
    }

    @Test
    void deserialisesNullAsNull() throws Exception {
        assertThat(mapper.readValue("{\"id\":null,\"title\":\"x\"}", Note.class).getId()).isNull();
    }

    @Test
    void reportsMalformedInputAsInvalidFormat() {
        assertThatExceptionOfType(InvalidFormatException.class)
                .isThrownBy(() -> mapper.readValue("\"not-a-ulid\"", Ulid.class))
                .withMessageContaining("not a valid ULID");
        assertThatExceptionOfType(MismatchedInputException.class)
                .isThrownBy(() -> mapper.readValue("12345", Ulid.class));
    }

    @Test
    void supportsMapKeys() throws Exception {
        Map<Ulid, String> map = Collections.singletonMap(ULID, "value");

        String json = mapper.writeValueAsString(map);
        assertThat(json).isEqualTo("{\"01ARZ3NDEKTSV4RRFFQ69G5FAV\":\"value\"}");
        assertThat(mapper.readValue(json, new TypeReference<Map<Ulid, String>>() { })).isEqualTo(map);

        assertThatExceptionOfType(InvalidFormatException.class)
                .isThrownBy(() -> mapper.readValue("{\"bad\":\"value\"}", new TypeReference<Map<Ulid, String>>() { }));
    }

    @Test
    void isDiscoveredThroughServiceLoader() throws Exception {
        ObjectMapper discovered = new ObjectMapper().findAndRegisterModules();

        assertThat(discovered.getRegisteredModuleIds()).contains(new UlidModule().getTypeId());
        assertThat(discovered.writeValueAsString(ULID)).isEqualTo("\"01ARZ3NDEKTSV4RRFFQ69G5FAV\"");
    }
}
