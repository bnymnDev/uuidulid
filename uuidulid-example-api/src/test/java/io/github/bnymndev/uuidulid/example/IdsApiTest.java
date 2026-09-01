package io.github.bnymndev.uuidulid.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bnymndev.uuidulid.Ulid;
import io.github.bnymndev.uuidulid.Uuids;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IdsApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void mintsUlids() throws Exception {
        JsonNode body = json.readTree(mvc.perform(get("/api/ids/ulid").param("count", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        List<Ulid> ulids = new ArrayList<>();
        body.forEach(node -> ulids.add(Ulid.parse(node.asText())));
        assertThat(ulids).hasSize(50).isSorted().doesNotHaveDuplicates();
    }

    @Test
    void mintsUuids() throws Exception {
        JsonNode v7 = json.readTree(mvc.perform(get("/api/ids/uuid7").param("count", "3"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode v4 = json.readTree(mvc.perform(get("/api/ids/uuid4"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        assertThat(v7).hasSize(3);
        v7.forEach(node -> assertThat(Uuids.isV7(UUID.fromString(node.asText()))).isTrue());
        assertThat(v4).hasSize(1);
        assertThat(UUID.fromString(v4.get(0).asText()).version()).isEqualTo(4);
    }

    @Test
    void clampsTheCount() throws Exception {
        mvc.perform(get("/api/ids/ulid").param("count", "0")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/ids/ulid").param("count", "999999")).andExpect(jsonPath("$.length()").value(1000));
    }

    @Test
    void inspectsAUlid() throws Exception {
        mvc.perform(get("/api/ids/inspect/{value}", "01ARZ3NDEKTSV4RRFFQ69G5FAV"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("ULID"))
                .andExpect(jsonPath("$.ulid").value("01ARZ3NDEKTSV4RRFFQ69G5FAV"))
                .andExpect(jsonPath("$.uuid").value("01563e3a-b5d3-d676-4c61-efb99302bd5b"))
                .andExpect(jsonPath("$.timestamp").value(1469922850259L))
                .andExpect(jsonPath("$.instant").value("2016-07-30T23:54:10.259Z"));
    }

    @Test
    void inspectsAUuid() throws Exception {
        mvc.perform(get("/api/ids/inspect/{value}", "017f22e2-79b0-7cc3-98c4-dc0c0c07398f"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("UUID"))
                .andExpect(jsonPath("$.uuidVersion").value(7))
                .andExpect(jsonPath("$.timestamp").value(1645557742000L))
                .andExpect(jsonPath("$.instant").value("2022-02-22T19:22:22Z"));

        mvc.perform(get("/api/ids/inspect/{value}", Uuids.v4().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuidVersion").value(4))
                .andExpect(jsonPath("$.timestamp").doesNotExist());
    }

    @Test
    void rejectsGarbage() throws Exception {
        mvc.perform(get("/api/ids/inspect/{value}", "garbage")).andExpect(status().isBadRequest());
    }
}
