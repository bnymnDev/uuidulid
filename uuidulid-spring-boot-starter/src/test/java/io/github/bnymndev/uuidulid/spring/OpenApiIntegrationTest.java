package io.github.bnymndev.uuidulid.spring;

import io.github.bnymndev.uuidulid.Ulid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With springdoc on the classpath, Ulid must appear as a plain string in the generated OpenAPI document. */
@SpringBootTest(classes = {OpenApiIntegrationTest.App.class, OpenApiIntegrationTest.ItemController.class})
@AutoConfigureMockMvc
class OpenApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void describesUlidPropertiesAsStrings() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.Item.properties.id.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.Item.properties.id.pattern").value(UlidOpenApi.PATTERN))
                .andExpect(jsonPath("$.components.schemas.Item.properties.id.maxLength").value(26))
                .andExpect(jsonPath("$.components.schemas.Item.properties.id.example").value(UlidOpenApi.EXAMPLE));
    }

    /** springdoc carries only the type over to parameter schemas; the pattern shows on properties. */
    @Test
    void describesUlidParametersAsStrings() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/items/{id}'].get.parameters[0].schema.type").value("string"))
                .andExpect(jsonPath("$.paths['/items/{id}'].get.parameters[0].schema.properties").doesNotExist());
    }

    @SpringBootApplication
    static class App {
    }

    record Item(Ulid id, String name) {
    }

    @RestController
    static class ItemController {

        @GetMapping("/items/{id}")
        Item one(@PathVariable Ulid id) {
            return new Item(id, "item");
        }
    }
}
