package io.github.bnymndev.uuidulid.hibernate;

import io.github.bnymndev.uuidulid.Ulid;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Boots a real Hibernate against H2 and proves Ulid works as an @Id in every physical form. */
class UlidJavaTypeTest {

    private static final Ulid ULID = Ulid.parse("01ARZ3NDEKTSV4RRFFQ69G5FAV");

    private static SessionFactory sessionFactory;

    @BeforeAll
    static void boot() {
        sessionFactory = new Configuration()
                .setProperty(AvailableSettings.JAKARTA_JDBC_URL, "jdbc:h2:mem:ulid;DB_CLOSE_DELAY=-1")
                .setProperty(AvailableSettings.HBM2DDL_AUTO, "create-drop")
                .addAnnotatedClass(TextKeyed.class)
                .addAnnotatedClass(BinaryKeyed.class)
                .addAnnotatedClass(UuidKeyed.class)
                .buildSessionFactory();
    }

    @AfterAll
    static void shutdown() {
        sessionFactory.close();
    }

    @Test
    void mapsUlidIdWithoutAnyAnnotationAsVarchar26() {
        sessionFactory.inTransaction(session -> session.persist(new TextKeyed(ULID, "text")));

        sessionFactory.inTransaction(session -> {
            assertThat(session.find(TextKeyed.class, ULID).name).isEqualTo("text");
            // The column really holds the canonical string, not serialised bytes.
            Object raw = session.createNativeQuery("select id from text_keyed", Object.class).getSingleResult();
            assertThat(raw).isEqualTo("01ARZ3NDEKTSV4RRFFQ69G5FAV");
        });
    }

    @Test
    void mapsUlidIdAsBinary16WhenAsked() {
        sessionFactory.inTransaction(session -> session.persist(new BinaryKeyed(ULID)));

        sessionFactory.inTransaction(session -> {
            assertThat(session.find(BinaryKeyed.class, ULID)).isNotNull();
            Object raw = session.createNativeQuery("select id from binary_keyed", Object.class).getSingleResult();
            assertThat(raw).isEqualTo(ULID.toBytes());
        });
    }

    @Test
    void mapsUlidIdAsNativeUuidWhenAsked() {
        sessionFactory.inTransaction(session -> session.persist(new UuidKeyed(ULID)));

        sessionFactory.inTransaction(session -> {
            assertThat(session.find(UuidKeyed.class, ULID)).isNotNull();
            // A real uuid column formats as the dashed form when cast; a BINARY(16) column would
            // format as plain hex instead.
            String raw = session.createNativeQuery("select cast(id as varchar) from uuid_keyed", String.class).getSingleResult();
            assertThat(raw).isEqualTo(ULID.toUuid().toString());
        });
    }

    @Test
    void ordersAndComparesByIdInQueries() {
        Ulid first = Ulid.of(1_700_000_000_000L, new byte[Ulid.RANDOMNESS_BYTES]);
        Ulid second = first.increment();
        Ulid third = second.increment();
        sessionFactory.inTransaction(session -> {
            session.persist(new TextKeyed(third, "c"));
            session.persist(new TextKeyed(first, "a"));
            session.persist(new TextKeyed(second, "b"));
        });

        sessionFactory.inTransaction(session -> {
            List<TextKeyed> after = session
                    .createQuery("from TextKeyed where id > :after and id <= :to order by id", TextKeyed.class)
                    .setParameter("after", first)
                    .setParameter("to", Ulid.max(1_700_000_000_000L))
                    .getResultList();
            assertThat(after).extracting(t -> t.name).containsExactly("b", "c");
        });
    }

    @Test
    void wrapsAndUnwrapsEveryPhysicalForm() {
        UlidJavaType type = UlidJavaType.INSTANCE;

        assertThat(type.unwrap(ULID, String.class, null)).isEqualTo(ULID.toString());
        assertThat(type.unwrap(ULID, byte[].class, null)).isEqualTo(ULID.toBytes());
        assertThat(type.unwrap(ULID, UUID.class, null)).isEqualTo(ULID.toUuid());
        assertThat(type.wrap(ULID.toString().toLowerCase(), null)).isEqualTo(ULID);
        assertThat(type.wrap(ULID.toBytes(), null)).isEqualTo(ULID);
        assertThat(type.wrap(ULID.toUuid(), null)).isEqualTo(ULID);
        assertThat(type.wrap(null, null)).isNull();
        assertThat(type.unwrap(null, String.class, null)).isNull();
    }

    @Entity(name = "TextKeyed")
    @Table(name = "text_keyed")
    static class TextKeyed {
        @Id
        Ulid id;
        String name;

        TextKeyed() {
        }

        TextKeyed(Ulid id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Entity(name = "BinaryKeyed")
    @Table(name = "binary_keyed")
    static class BinaryKeyed {
        @Id
        @JdbcTypeCode(SqlTypes.BINARY)
        Ulid id;

        BinaryKeyed() {
        }

        BinaryKeyed(Ulid id) {
            this.id = id;
        }
    }

    @Entity(name = "UuidKeyed")
    @Table(name = "uuid_keyed")
    static class UuidKeyed {
        @Id
        @JdbcTypeCode(SqlTypes.UUID)
        Ulid id;

        UuidKeyed() {
        }

        UuidKeyed(Ulid id) {
            this.id = id;
        }
    }
}
