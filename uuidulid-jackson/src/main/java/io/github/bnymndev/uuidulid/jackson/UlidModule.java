package io.github.bnymndev.uuidulid.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.bnymndev.uuidulid.Ulid;

/**
 * Jackson module that maps {@link Ulid} to and from its 26-character string form, both as a
 * value and as a {@link java.util.Map} key.
 *
 * <p>Register it explicitly:
 * <pre>{@code
 * ObjectMapper mapper = new ObjectMapper().registerModule(new UlidModule());
 * }</pre>
 *
 * <p>or let Jackson discover it via {@link java.util.ServiceLoader}:
 * <pre>{@code
 * ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
 * }</pre>
 *
 * <p>With {@code uuidulid-spring-boot-starter} on the classpath the module is registered with
 * Spring Boot's {@code ObjectMapper} automatically.
 */
public final class UlidModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public UlidModule() {
        super("uuidulid", Version.unknownVersion());
        addSerializer(Ulid.class, new UlidSerializer());
        addDeserializer(Ulid.class, new UlidDeserializer());
        addKeySerializer(Ulid.class, new UlidKeySerializer());
        addKeyDeserializer(Ulid.class, new UlidKeyDeserializer());
    }
}
