package io.github.bnymndev.uuidulid.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bnymndev.uuidulid.Ulid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotesApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void createsANoteAndHandsOutItsUlid() throws Exception {
        MvcResult created = mvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hello\",\"body\":\"first note\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.title").value("Hello"))
                .andReturn();

        JsonNode body = json.readTree(created.getResponse().getContentAsString());
        Ulid id = Ulid.parse(body.get("id").asText());
        assertThat(created.getResponse().getHeader("Location")).endsWith("/api/notes/" + id);
        assertThat(Instant.parse(body.get("createdAt").asText())).isEqualTo(id.getInstant());

        mvc.perform(get("/api/notes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.body").value("first note"));

        // Lookup is case-insensitive, as the ULID spec requires.
        mvc.perform(get("/api/notes/{id}", id.toString().toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void reportsMalformedIdsAsProblemDetails() throws Exception {
        mvc.perform(get("/api/notes/{id}", "definitely-not-a-ulid"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.title").value("Invalid identifier"))
                .andExpect(jsonPath("$.detail").value("'definitely-not-a-ulid' is not a valid ULID"));
    }

    @Test
    void reportsUnknownIdsAsNotFound() throws Exception {
        Ulid unknown = Ulid.random();

        mvc.perform(get("/api/notes/{id}", unknown))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No note with id " + unknown));
        mvc.perform(delete("/api/notes/{id}", unknown)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidBodies() throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\",\"body\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pagesThroughNotesInCreationOrderUsingTheIdAsCursor() throws Exception {
        List<Ulid> created = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            created.add(create("paging " + i));
        }

        // Page 1: the two oldest of this batch, using the id before the batch as the cursor.
        Ulid before = created.get(0);
        JsonNode page1 = json.readTree(mvc.perform(get("/api/notes").param("after", before.toString()).param("limit", "2"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(ids(page1)).containsExactly(created.get(1), created.get(2));
        assertThat(page1.get("nextCursor").asText()).isEqualTo(created.get(2).toString());

        // Page 2 continues from the cursor and is the last page.
        JsonNode page2 = json.readTree(mvc.perform(get("/api/notes").param("after", page1.get("nextCursor").asText()).param("limit", "2"))
                .andReturn().getResponse().getContentAsString());
        assertThat(ids(page2)).containsExactly(created.get(3), created.get(4));
        assertThat(page2.get("nextCursor").isNull()).isTrue();

        // Ids are strictly increasing in creation order: the monotonic factory at work.
        assertThat(created).isSorted().doesNotHaveDuplicates();
    }

    @Test
    void filtersByCreationTimeUsingTheIdRange() throws Exception {
        Ulid first = create("range 1");
        Thread.sleep(5);
        Ulid second = create("range 2");
        Thread.sleep(5);
        Ulid third = create("range 3");

        JsonNode page = json.readTree(mvc.perform(get("/api/notes")
                        .param("from", second.getInstant().toString())
                        .param("to", second.getInstant().toString())
                        .param("limit", "100"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertThat(ids(page)).contains(second).doesNotContain(first, third);
    }

    @Test
    void deletesNotes() throws Exception {
        Ulid id = create("to delete");

        mvc.perform(delete("/api/notes/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/notes/{id}", id)).andExpect(status().isNotFound());
    }

    private Ulid create(String title) throws Exception {
        String body = mvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"body\":\"body\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Ulid.parse(json.readTree(body).get("id").asText());
    }

    private static List<Ulid> ids(JsonNode page) {
        List<Ulid> ids = new ArrayList<>();
        page.get("items").forEach(item -> ids.add(Ulid.parse(item.get("id").asText())));
        return ids;
    }
}
