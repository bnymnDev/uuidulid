package io.github.bnymndev.uuidulid.springboot2;

import io.github.bnymndev.uuidulid.UlidFactory;
import io.github.bnymndev.uuidulid.Uuid7Factory;
import io.github.bnymndev.uuidulid.jackson.UlidModule;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for Spring Boot 2.x. Registered through {@code META-INF/spring.factories}.
 *
 * <p>Provides a shared {@link UlidFactory} and {@link Uuid7Factory}, converters so that
 * {@code @PathVariable Ulid} and {@code @RequestParam Ulid} bind in Spring MVC and WebFlux, and
 * the Jackson {@link UlidModule} so {@code Ulid} fields serialise as strings. Each bean backs off
 * if the application defines its own.
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(JacksonAutoConfiguration.class)
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
