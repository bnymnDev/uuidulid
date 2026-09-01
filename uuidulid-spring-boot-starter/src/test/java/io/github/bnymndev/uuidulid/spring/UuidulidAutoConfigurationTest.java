package io.github.bnymndev.uuidulid.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bnymndev.uuidulid.Ulid;
import io.github.bnymndev.uuidulid.UlidFactory;
import io.github.bnymndev.uuidulid.Uuid7Factory;
import io.github.bnymndev.uuidulid.jackson.UlidModule;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class UuidulidAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(UuidulidAutoConfiguration.class, JacksonAutoConfiguration.class));

    @Test
    void providesMonotonicFactoriesByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(UlidFactory.class).hasSingleBean(Uuid7Factory.class);
            assertThat(context.getBean(UlidFactory.class).isMonotonic()).isTrue();
            assertThat(context.getBean(Uuid7Factory.class).isMonotonic()).isTrue();
        });
    }

    @Test
    void honoursTheMonotonicProperty() {
        runner.withPropertyValues("uuidulid.monotonic=false").run(context -> {
            assertThat(context.getBean(UlidFactory.class).isMonotonic()).isFalse();
            assertThat(context.getBean(Uuid7Factory.class).isMonotonic()).isFalse();
        });
    }

    @Test
    void backsOffWhenTheApplicationDefinesItsOwnFactory() {
        runner.withUserConfiguration(CustomFactoryConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(UlidFactory.class);
            assertThat(context.getBean(UlidFactory.class)).isSameAs(CustomFactoryConfiguration.FACTORY);
        });
    }

    @Test
    void factoryIsInjectableAsASupplier() {
        runner.run(context -> {
            @SuppressWarnings("unchecked")
            Supplier<Ulid> supplier = context.getBean(UlidFactory.class);
            assertThat(supplier.get()).isNotNull();
        });
    }

    @Test
    void registersTheJacksonModuleWithTheObjectMapper() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(UlidModule.class);
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            Ulid ulid = Ulid.parse("01ARZ3NDEKTSV4RRFFQ69G5FAV");

            assertThat(mapper.writeValueAsString(ulid)).isEqualTo("\"01ARZ3NDEKTSV4RRFFQ69G5FAV\"");
            assertThat(mapper.readValue("\"01ARZ3NDEKTSV4RRFFQ69G5FAV\"", Ulid.class)).isEqualTo(ulid);
        });
    }

    @Test
    void providesConvertersInBothDirections() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(StringToUlidConverter.class).hasSingleBean(UlidToStringConverter.class);
            Ulid ulid = Ulid.parse("01ARZ3NDEKTSV4RRFFQ69G5FAV");

            assertThat(context.getBean(StringToUlidConverter.class).convert(" 01arz3ndektsv4rrffq69g5fav ")).isEqualTo(ulid);
            assertThat(context.getBean(StringToUlidConverter.class).convert("   ")).isNull();
            assertThat(context.getBean(UlidToStringConverter.class).convert(ulid)).isEqualTo("01ARZ3NDEKTSV4RRFFQ69G5FAV");
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomFactoryConfiguration {

        static final UlidFactory FACTORY = UlidFactory.random();

        @Bean
        UlidFactory ulidFactory() {
            return FACTORY;
        }
    }
}
