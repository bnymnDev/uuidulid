package io.github.bnymndev.uuidulid.example.postgres;

import io.github.bnymndev.uuidulid.Uuids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * A note keyed by a UUIDv7 that the application assigns before the insert.
 *
 * <p>{@link UUID} is a standard JPA type, so the {@code uuid} column needs no converter. The
 * creation time is read straight out of the id.
 *
 * <p>Implements {@link Persistable} because the id is assigned by the application: without
 * {@link #isNew()} Spring Data would issue a {@code SELECT} before every insert to find out whether
 * the row already exists.
 */
@Entity
@Table(name = "note")
public class Note implements Persistable<UUID> {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String body;

    @Transient
    private boolean isNew;

    protected Note() {
    }

    public Note(UUID id, String title, String body) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.isNew = true;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return Uuids.instant(id);
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
}
