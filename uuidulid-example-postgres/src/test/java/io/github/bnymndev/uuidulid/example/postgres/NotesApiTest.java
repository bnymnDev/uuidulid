package io.github.bnymndev.uuidulid.example.postgres;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bnymndev.uuidulid.Ulid;
import io.github.bnymndev.uuidulid.Uuids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End to end against a real PostgreSQL 17: uuid column inside, ULIDs outside. */
@SpringBootTest
@AutoConfigureMockMvc
class NotesApiTest {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        EmbeddedPostgresSupport.register(registry);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    @BeforeEach
    void emptyTable() {
        jdbc.update("delete from note");
    }

    @Test
    void storesAUuidV7AndHandsOutTheSameBitsAsAUlid() throws Exception {
        String id = create("first", "a");

        Ulid ulid = Ulid.parse(id);
        UUID stored = jdbc.queryForObject("select id from note", UUID.class);

        assertThat(id).hasSize(26);
        assertThat(stored).isEqualTo(ulid.toUuid());
        assertThat(stored.version()).isEqualTo(7);
        assertThat(Uuids.isV7(ulid.toUuid())).isTrue();
        // The column really is PostgreSQL's native uuid type.
        assertThat(jdbc.queryForObject(
                "select data_type from information_schema.columns where table_name = 'note' and column_name = 'id'",
                String.class)).isEqualTo("uuid");
    }

    @Test
    void readsByUlidInAnyCase() throws Exception {
        String id = create("first", "a");

        mvc.perform(get("/api/notes/{id}", id.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("first"))
                .andExpect(jsonPath("$.createdAt").value(Uuids.instant(Ulid.parse(id).toUuid()).toString()));
    }

    @Test
    void rejectsUlidsThatCannotHaveBeenIssuedHere() throws Exception {
        // A random ULID is well-formed but does not encode a UUIDv7.
        mvc.perform(get("/api/notes/{id}", Ulid.random()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid identifier"));
    }

    @Test
    void unknownButPlausibleIdIs404() throws Exception {
        mvc.perform(get("/api/notes/{id}", Ulid.fromUuid(Uuids.v7())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Note not found"));
    }

    @Test
    void malformedIdIs400() throws Exception {
        mvc.perform(get("/api/notes/{id}", "not-an-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("'not-an-id' is not a valid Ulid"));
    }

    @Test
    void validatesTheRequestBody() throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void paginatesInCreationOrderStraightFromPostgres() throws Exception {
        List<String> created = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            created.add(create("note " + i, null));
        }

        // What PostgreSQL considers the order of the uuid column ...
        List<UUID> databaseOrder = jdbc.queryForList("select id from note order by id", UUID.class);
        // ... is the order of the ULID strings, and the order of creation.
        assertThat(databaseOrder.stream().map(Ulid::fromUuid).map(Ulid::toString).toList()).isEqualTo(created);
        assertThat(created).isSorted();

        JsonNode page1 = json.readTree(mvc.perform(get("/api/notes").param("limit", "2"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(ids(page1)).containsExactly(created.get(0), created.get(1));
        assertThat(page1.get("nextCursor").asText()).isEqualTo(created.get(1));

        JsonNode page2 = json.readTree(mvc.perform(get("/api/notes").param("limit", "2").param("after", page1.get("nextCursor").asText()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(ids(page2)).containsExactly(created.get(2), created.get(3));

        JsonNode page3 = json.readTree(mvc.perform(get("/api/notes").param("limit", "2").param("after", page2.get("nextCursor").asText()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(ids(page3)).containsExactly(created.get(4));
        assertThat(page3.get("nextCursor").isNull()).isTrue();
    }

    @Test
    void filtersByCreationTimeUsingOnlyThePrimaryKey() throws Exception {
        String first = create("old", null);
        Thread.sleep(5);
        Instant cutoff = Instant.now();
        Thread.sleep(5);
        String second = create("new", null);
        String third = create("newer", null);

        JsonNode recent = json.readTree(mvc.perform(get("/api/notes").param("from", cutoff.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(ids(recent)).containsExactly(second, third);

        JsonNode old = json.readTree(mvc.perform(get("/api/notes").param("to", cutoff.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(ids(old)).containsExactly(first);
    }

    @Test
    void deletes() throws Exception {
        String id = create("gone", null);

        mvc.perform(delete("/api/notes/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/notes/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void inspectShowsBothSpellingsOfOneIdentifier() throws Exception {
        String id = create("x", null);
        UUID uuid = Ulid.parse(id).toUuid();

        mvc.perform(get("/api/ids/inspect/{value}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ulid").value(id))
                .andExpect(jsonPath("$.uuid").value(uuid.toString()))
                .andExpect(jsonPath("$.uuidVersion").value(7))
                .andExpect(jsonPath("$.issuedByThisService").value(true));

        mvc.perform(get("/api/ids/inspect/{value}", uuid.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ulid").value(id));
    }

    private String create(String title, String body) throws Exception {
        String request = json.writeValueAsString(new NoteDtos.NoteRequest(title, body));
        String response = mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/notes/")))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asText();
    }

    private static List<String> ids(JsonNode page) {
        List<String> ids = new ArrayList<>();
        page.get("items").forEach(item -> ids.add(item.get("id").asText()));
        return ids;
    }
}
