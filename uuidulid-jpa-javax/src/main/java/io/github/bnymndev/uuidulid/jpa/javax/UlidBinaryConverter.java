package io.github.bnymndev.uuidulid.jpa.javax;

import io.github.bnymndev.uuidulid.Ulid;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Maps a {@link Ulid} to its 16-byte big-endian binary form, for example {@code BINARY(16)}.
 *
 * <p>Big-endian byte order preserves the sort order of the identifier, so an index on the column
 * is still ordered by creation time. This converter is not auto-applied because only one
 * auto-apply converter may exist per attribute type; enable it per attribute:
 *
 * <pre>{@code
 * @Id
 * @Convert(converter = UlidBinaryConverter.class)
 * @Column(length = 16, columnDefinition = "BINARY(16)")
 * private Ulid id;
 * }</pre>
 */
@Converter
public class UlidBinaryConverter implements AttributeConverter<Ulid, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(Ulid attribute) {
        return attribute == null ? null : attribute.toBytes();
    }

    @Override
    public Ulid convertToEntityAttribute(byte[] dbData) {
        return dbData == null ? null : Ulid.fromBytes(dbData);
    }
}
