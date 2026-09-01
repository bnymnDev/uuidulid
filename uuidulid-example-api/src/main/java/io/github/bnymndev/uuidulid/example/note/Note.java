package io.github.bnymndev.uuidulid.example.note;

import io.github.bnymndev.uuidulid.Ulid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

/**
 * A note whose primary key is a ULID assigned by the application, not by the database.
 *
 * <p>No mapping annotation is needed for the id: {@code uuidulid-hibernate} registers a Hibernate
 * type for {@code Ulid} that defaults to {@code VARCHAR(26)}. (JPA {@code AttributeConverter}s
 * are not applied to {@code @Id} attributes, which is why the plain converter module is not
 * enough here.)
 *
 * <p>Two things are worth noting:
 * <ul>
 *   <li>The identifier is known before the row is inserted, so the API can return it (and a
 *       {@code Location} header) without a round trip, and batch inserts need no generated-key
 *       retrieval.</li>
 *   <li>Because the ULID embeds its creation time, no separate {@code created_at} column is
 *       needed: see {@link #getCreatedAt()}.</li>
 * </ul>
 *
 * <p>Implementing {@link Persistable} tells Spring Data that a freshly constructed note is new
 * even though its id is set. Without it {@code save()} would issue a {@code SELECT} first to find
 * out.
 */
@Entity
@Table(name = "notes")
public class Note implements Persistable<Ulid> {

    @Id
    @Column(nullable = false, updatable = false)
    private Ulid id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String body;

    @Transient
    private boolean isNew = true;

    /** For JPA. */
    protected Note() {
    }

    public Note(Ulid id, String title, String body) {
        this.id = id;
        this.title = title;
        this.body = body;
    }

    @Override
    public Ulid getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    /** The creation time, read straight from the identifier. */
    public Instant getCreatedAt() {
        return id.getInstant();
    }
}
