package io.github.bnymndev.uuidulid.example.note;

import io.github.bnymndev.uuidulid.Ulid;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Because the id column is ordered by creation time, both keyset pagination and time-range
 * queries are plain comparisons on the primary key index.
 */
public interface NoteRepository extends JpaRepository<Note, Ulid> {

    /** First page: the oldest notes. */
    List<Note> findAllByOrderByIdAsc(Limit limit);

    /** Following pages: everything created after the cursor. */
    List<Note> findByIdGreaterThanOrderByIdAsc(Ulid after, Limit limit);

    /** Notes created within {@code [from, to]}; pair with {@link Ulid#min} and {@link Ulid#max}. */
    List<Note> findByIdBetweenOrderByIdAsc(Ulid from, Ulid to, Limit limit);
}
