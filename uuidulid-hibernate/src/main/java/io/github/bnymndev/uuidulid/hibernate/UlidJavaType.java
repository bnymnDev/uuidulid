package io.github.bnymndev.uuidulid.hibernate;

import io.github.bnymndev.uuidulid.Ulid;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import java.sql.Types;
import java.util.UUID;

/**
 * Hibernate {@link org.hibernate.type.descriptor.java.JavaType} for {@link Ulid}.
 *
 * <p>Registered globally by {@link UlidTypeContributor}, so any {@code Ulid} attribute &mdash;
 * including {@code @Id} attributes, which JPA {@code AttributeConverter}s cannot cover &mdash;
 * maps without further annotations. The default column is {@code VARCHAR(26)} holding the
 * canonical text form. Pick a different physical representation per attribute with Hibernate's
 * {@code @JdbcTypeCode}:
 *
 * <pre>{@code
 * @Id private Ulid id;                                   // VARCHAR(26)
 * @Id @JdbcTypeCode(SqlTypes.BINARY) private Ulid id;    // BINARY(16), big-endian, sortable
 * @Id @JdbcTypeCode(SqlTypes.UUID)   private Ulid id;    // native uuid column (e.g. PostgreSQL)
 * }</pre>
 */
public class UlidJavaType extends AbstractClassJavaType<Ulid> {

    private static final long serialVersionUID = 1L;

    public static final UlidJavaType INSTANCE = new UlidJavaType();

    public UlidJavaType() {
        super(Ulid.class);
    }

    @Override
    public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
        return indicators.getJdbcType(Types.VARCHAR);
    }

    @Override
    public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
        return jdbcType.isBinary() ? Ulid.BYTES : Ulid.LENGTH;
    }

    @Override
    public String toString(Ulid value) {
        return value.toString();
    }

    @Override
    public Ulid fromString(CharSequence string) {
        return Ulid.parse(string);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> X unwrap(Ulid value, Class<X> type, WrapperOptions options) {
        if (value == null) {
            return null;
        }
        if (Ulid.class.isAssignableFrom(type)) {
            return (X) value;
        }
        if (String.class.isAssignableFrom(type)) {
            return (X) value.toString();
        }
        if (byte[].class.isAssignableFrom(type)) {
            return (X) value.toBytes();
        }
        if (UUID.class.isAssignableFrom(type)) {
            return (X) value.toUuid();
        }
        throw unknownUnwrap(type);
    }

    @Override
    public <X> Ulid wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null;
        }
        if (value instanceof Ulid) {
            return (Ulid) value;
        }
        if (value instanceof CharSequence) {
            return Ulid.parse(value.toString().trim());
        }
        if (value instanceof byte[]) {
            return Ulid.fromBytes((byte[]) value);
        }
        if (value instanceof UUID) {
            return Ulid.fromUuid((UUID) value);
        }
        throw unknownWrap(value.getClass());
    }
}
