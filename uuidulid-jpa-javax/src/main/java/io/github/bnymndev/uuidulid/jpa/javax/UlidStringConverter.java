package io.github.bnymndev.uuidulid.jpa.javax;

import io.github.bnymndev.uuidulid.Ulid;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Maps a {@link Ulid} to a 26-character string column, for example {@code CHAR(26)}.
 *
 * <p>This is the default mapping: it is human-readable in the database, sorts correctly with any
 * binary or ASCII collation, and works with every JPA provider. Use
 * {@link UlidBinaryConverter} to trade readability for a compact {@code BINARY(16)} column.
 *
 * <p>The converter is marked {@code autoApply}, so a JPA provider that scans this package applies
 * it to every {@code Ulid} attribute automatically. Spring Boot only scans the application's own
 * packages by default; either extend the scan with
 * {@code @EntityScan(basePackageClasses = {MyApp.class, UlidStringConverter.class})} or apply the
 * converter explicitly:
 *
 * <pre>{@code
 * @Id
 * @Convert(converter = UlidStringConverter.class)
 * @Column(length = 26, columnDefinition = "CHAR(26)")
 * private Ulid id;
 * }</pre>
 */
@Converter(autoApply = true)
public class UlidStringConverter implements AttributeConverter<Ulid, String> {

    @Override
    public String convertToDatabaseColumn(Ulid attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public Ulid convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Ulid.parse(dbData.trim());
    }
}
