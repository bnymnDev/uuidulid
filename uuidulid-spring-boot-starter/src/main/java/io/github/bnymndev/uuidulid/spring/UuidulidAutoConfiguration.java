package io.github.bnymndev.uuidulid.spring;

import io.github.bnymndev.uuidulid.UlidFactory;
import io.github.bnymndev.uuidulid.Uuid7Factory;
import io.github.bnymndev.uuidulid.jackson.UlidModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration that makes ULIDs a first-class type in a Spring Boot application:
 *
 * <ul>
 *   <li>a shared {@link UlidFactory} and {@link Uuid7Factory} bean, monotonic unless
 *       {@code uuidulid.monotonic=false};</li>
 *   <li>{@link org.springframework.core.convert.converter.Converter Converters} so that
 *       {@code @PathVariable Ulid id} and {@code @RequestParam Ulid after} work in Spring MVC
 *       and WebFlux, and {@code Ulid} properties bind from configuration;</li>
 *   <li>the Jackson {@link UlidModule}, so that {@code Ulid} fields serialise as strings in
 *       every {@code @RestController} response and request body.</li>
 * </ul>
 *
 * <p>Every bean is conditional on the application not defining its own of the same type.
 */
@AutoConfiguration(before = JacksonAutoConfiguration.class)
@EnableConfigurationProperties(UuidulidProperties.class)
public class UuidulidAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public UlidFactory ulidFactory(UuidulidProperties properties) {
        return UlidFactory.builder().monotonic(properties.isMonotonic()).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public Uuid7Factory uuid7Factory(UuidulidProperties properties) {
        return Uuid7Factory.builder().monotonic(properties.isMonotonic()).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public StringToUlidConverter stringToUlidConverter() {
        return new StringToUlidConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public UlidToStringConverter ulidToStringConverter() {
        return new UlidToStringConverter();
    }

    /** Registers the Jackson module when Jackson is on the classpath. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
    static class JacksonConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public UlidModule ulidJacksonModule() {
            return new UlidModule();
        }
    }
}
