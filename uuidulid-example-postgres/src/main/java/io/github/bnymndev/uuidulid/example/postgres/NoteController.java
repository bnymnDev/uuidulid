package io.github.bnymndev.uuidulid.example.postgres;

import io.github.bnymndev.uuidulid.Ulid;
import io.github.bnymndev.uuidulid.Uuid7Factory;
import io.github.bnymndev.uuidulid.Uuids;
import io.github.bnymndev.uuidulid.example.postgres.NoteDtos.NotePage;
import io.github.bnymndev.uuidulid.example.postgres.NoteDtos.NoteRequest;
import io.github.bnymndev.uuidulid.example.postgres.NoteDtos.NoteResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Limit;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Notes API. The controller only ever sees {@link Ulid}; {@link PublicId} translates at the edge.
 */
@RestController
@RequestMapping("/api/notes")
class NoteController {

    private static final int MAX_LIMIT = 100;

    private final NoteRepository notes;
    private final Uuid7Factory ids;

    NoteController(NoteRepository notes, Uuid7Factory ids) {
        this.notes = notes;
        this.ids = ids;
    }

    @PostMapping
    ResponseEntity<NoteResponse> create(@Valid @RequestBody NoteRequest request) {
        Note note = notes.save(new Note(ids.create(), request.title(), request.body()));
        NoteResponse body = NoteResponse.from(note);
        return ResponseEntity.created(URI.create("/api/notes/" + body.id())).body(body);
    }

    @GetMapping("/{id}")
    NoteResponse get(@PathVariable Ulid id) {
        return NoteResponse.from(find(id));
    }

    /**
     * Lists notes in creation order.
     *
     * @param after exclusive cursor: the id of the last note of the previous page
     * @param from  inclusive lower bound on creation time
     * @param to    inclusive upper bound on creation time
     * @param limit page size, 1 to 100
     */
    @GetMapping
    NotePage list(@RequestParam(required = false) Ulid after,
                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                  @RequestParam(defaultValue = "20") int limit) {
        int size = Math.max(1, Math.min(limit, MAX_LIMIT));
        Limit pageLimit = Limit.of(size);

        List<Note> page;
        if (from != null || to != null) {
            // A time window is a key range: the smallest and largest ids possible at those instants.
            UUID lower = from != null ? Ulid.min(from).toUuid() : Uuids.NIL;
            UUID upper = to != null ? Ulid.max(to).toUuid() : Uuids.MAX;
            if (after != null) {
                UUID cursor = PublicId.toUuid(after);
                if (Uuids.compareUnsigned(cursor, lower) >= 0) {
                    lower = Ulid.fromUuid(cursor).increment().toUuid();
                }
            }
            page = notes.findByIdBetweenOrderByIdAsc(lower, upper, pageLimit);
        } else if (after != null) {
            page = notes.findByIdGreaterThanOrderByIdAsc(PublicId.toUuid(after), pageLimit);
        } else {
            page = notes.findAllByOrderByIdAsc(pageLimit);
        }

        List<NoteResponse> items = page.stream().map(NoteResponse::from).toList();
        Ulid nextCursor = items.size() == size ? items.get(items.size() - 1).id() : null;
        return new NotePage(items, nextCursor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Ulid id) {
        notes.delete(find(id));
    }

    private Note find(Ulid id) {
        return notes.findById(PublicId.toUuid(id)).orElseThrow(() -> new NoteNotFoundException(id));
    }
}
