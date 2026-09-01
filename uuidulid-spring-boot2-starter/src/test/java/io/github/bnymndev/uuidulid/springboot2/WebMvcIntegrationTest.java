package io.github.bnymndev.uuidulid.springboot2;

import io.github.bnymndev.uuidulid.Ulid;
import io.github.bnymndev.uuidulid.UlidFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs the starter through a real Boot 2 MVC stack: path variables, request parameters and JSON bodies. */
@SpringBootTest(classes = {WebMvcIntegrationTest.App.class, WebMvcIntegrationTest.ItemController.class})
@AutoConfigureMockMvc
class WebMvcIntegrationTest {

    private static final String ULID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Autowired
    private MockMvc mvc;

    @Test
    void bindsPathVariablesAndSerialisesResponses() throws Exception {
        mvc.perform(get("/items/{id}", ULID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ULID))
                .andExpect(jsonPath("$.timestamp").value(1469922850259L));
    }

    @Test
    void bindsRequestParameters() throws Exception {
        mvc.perform(get("/items").param("after", ULID.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.after").value(ULID));
    }

    @Test
    void rejectsMalformedPathVariables() throws Exception {
        mvc.perform(get("/items/{id}", "not-a-ulid")).andExpect(status().isBadRequest());
    }

    @Test
    void readsAndWritesJsonBodies() throws Exception {
        mvc.perform(post("/items").contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"" + ULID + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echoed").value(ULID))
                .andExpect(jsonPath("$.generated").isString());
    }

    @Test
    void rejectsMalformedJsonBodies() throws Exception {
        mvc.perform(post("/items").contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"nope\"}"))
                .andExpect(status().isBadRequest());
    }

    @SpringBootApplication
    static class App {
    }

    static class Item {
        private Ulid id;

        public Ulid getId() {
            return id;
        }

        public void setId(Ulid id) {
            this.id = id;
        }
    }

    @RestController
    static class ItemController {

        private final UlidFactory ulids;

        ItemController(UlidFactory ulids) {
            this.ulids = ulids;
        }

        @GetMapping("/items/{id}")
        Map<String, Object> one(@PathVariable Ulid id) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", id);
            body.put("timestamp", id.getTimestamp());
            return body;
        }

        @GetMapping("/items")
        Map<String, Object> list(@RequestParam Ulid after) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("after", after);
            return body;
        }

        @PostMapping("/items")
        Map<String, Object> create(@RequestBody Item item) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("echoed", item.getId());
            body.put("generated", ulids.create());
            return body;
        }
    }
}
