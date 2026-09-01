package io.github.bnymndev.uuidulid.example.note;

import io.github.bnymndev.uuidulid.Ulid;
import io.github.bnymndev.uuidulid.UlidFactory;
import jakarta.validation.Valid;
import org.springframework.data.domain.Limit;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/notes")
class NoteController {

    private static final int MAX_PAGE_SIZE = 100;

    private final NoteRepository notes;
    private final UlidFactory ulids;

    NoteController(NoteRepository notes, UlidFactory ulids) {
        this.notes = notes;
        this.ulids = ulids;
    }

    /** Creates a note. The ULID is generated here, before anything touches the database. */
    @PostMapping
    ResponseEntity<NoteResponse> create(@Valid @RequestBody NoteRequest request) {
        Note note = notes.save(new Note(ulids.create(), request.title(), request.body()));
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(note.getId());
        return ResponseEntity.created(location).body(NoteResponse.from(note));
    }

    @GetMapping("/{id}")
    NoteResponse get(@PathVariable Ulid id) {
        return notes.findById(id).map(NoteResponse::from).orElseThrow(() -> new NoteNotFoundException(id));
    }

    /**
     * Lists notes oldest first. Supports keyset pagination via {@code after} and a creation-time
     * window via {@code from}/{@code to}, all of which are comparisons on the primary key.
     */
    @GetMapping
    NotePage list(
            @RequestParam(required = false) Ulid after,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "20") int limit) {
        int pageSize = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
        // Fetch one extra row to know whether another page exists.
        Limit probe = Limit.of(pageSize + 1);

        List<Note> rows;
        if (from != null || to != null) {
            Ulid lower = after != null ? after.increment() : from != null ? Ulid.min(from) : Ulid.MIN;
            Ulid upper = to != null ? Ulid.max(to) : Ulid.MAX;
            rows = notes.findByIdBetweenOrderByIdAsc(lower, upper, probe);
        } else if (after != null) {
            rows = notes.findByIdGreaterThanOrderByIdAsc(after, probe);
        } else {
            rows = notes.findAllByOrderByIdAsc(probe);
        }

        boolean hasMore = rows.size() > pageSize;
        List<NoteResponse> items = rows.stream().limit(pageSize).map(NoteResponse::from).toList();
        Ulid nextCursor = hasMore ? items.get(items.size() - 1).id() : null;
        return new NotePage(items, nextCursor);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Ulid id) {
        if (!notes.existsById(id)) {
            throw new NoteNotFoundException(id);
        }
        notes.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
