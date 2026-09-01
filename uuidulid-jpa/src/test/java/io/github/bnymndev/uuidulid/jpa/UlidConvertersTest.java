package io.github.bnymndev.uuidulid.jpa;

import io.github.bnymndev.uuidulid.Ulid;
import jakarta.persistence.Converter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UlidConvertersTest {

    private static final Ulid ULID = Ulid.parse("01ARZ3NDEKTSV4RRFFQ69G5FAV");

    private final UlidStringConverter stringConverter = new UlidStringConverter();
    private final UlidBinaryConverter binaryConverter = new UlidBinaryConverter();

    @Test
    void stringConverterRoundTrips() {
        String column = stringConverter.convertToDatabaseColumn(ULID);

        assertThat(column).isEqualTo("01ARZ3NDEKTSV4RRFFQ69G5FAV").hasSize(Ulid.LENGTH);
        assertThat(stringConverter.convertToEntityAttribute(column)).isEqualTo(ULID);
        // CHAR columns come back space-padded from some databases.
        assertThat(stringConverter.convertToEntityAttribute(column + "  ")).isEqualTo(ULID);
    }

    @Test
    void binaryConverterRoundTripsAndPreservesOrder() {
        byte[] column = binaryConverter.convertToDatabaseColumn(ULID);

        assertThat(column).hasSize(Ulid.BYTES);
        assertThat(binaryConverter.convertToEntityAttribute(column)).isEqualTo(ULID);

        byte[] later = binaryConverter.convertToDatabaseColumn(ULID.increment());
        assertThat(new java.math.BigInteger(1, column)).isLessThan(new java.math.BigInteger(1, later));
    }

    @Test
    void convertersPassNullThrough() {
        assertThat(stringConverter.convertToDatabaseColumn(null)).isNull();
        assertThat(stringConverter.convertToEntityAttribute(null)).isNull();
        assertThat(binaryConverter.convertToDatabaseColumn(null)).isNull();
        assertThat(binaryConverter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void onlyTheStringConverterIsAutoApplied() {
        assertThat(UlidStringConverter.class.getAnnotation(Converter.class).autoApply()).isTrue();
        assertThat(UlidBinaryConverter.class.getAnnotation(Converter.class).autoApply()).isFalse();
    }
}
