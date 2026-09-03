package io.github.bnymndev.uuidulid.example.postgres;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * All ordering and range conditions run on the {@code uuid} primary key. PostgreSQL compares
 * {@code uuid} values byte-wise, which is exactly the order of UUIDv7 creation time and of the
 * ULID strings handed out by the API.
 */
interface NoteRepository extends JpaRepository<Note, UUID> {

    List<Note> findAllByOrderByIdAsc(Limit limit);

    List<Note> findByIdGreaterThanOrderByIdAsc(UUID after, Limit limit);

    List<Note> findByIdBetweenOrderByIdAsc(UUID from, UUID to, Limit limit);
}
