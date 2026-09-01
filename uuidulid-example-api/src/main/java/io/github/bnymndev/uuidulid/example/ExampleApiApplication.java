package io.github.bnymndev.uuidulid.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A small REST API that hands out a ULID for every record it stores.
 *
 * <p>Run it with {@code mvn -pl uuidulid-example-api spring-boot:run} and try:
 * <pre>
 * curl -X POST localhost:8080/api/notes -H 'Content-Type: application/json' \
 *      -d '{"title":"Hello","body":"first note"}'
 * curl localhost:8080/api/notes
 * curl localhost:8080/api/ids/inspect/01ARZ3NDEKTSV4RRFFQ69G5FAV
 * </pre>
 */
@SpringBootApplication
public class ExampleApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleApiApplication.class, args);
    }
}
