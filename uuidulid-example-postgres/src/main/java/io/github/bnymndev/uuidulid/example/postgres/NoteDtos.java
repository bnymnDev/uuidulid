package io.github.bnymndev.uuidulid.example.postgres;

import io.github.bnymndev.uuidulid.Ulid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request and response shapes. Identifiers are {@link Ulid} here and {@link UUID} in the entity. */
final class NoteDtos {

    private NoteDtos() {
    }

    record NoteRequest(@NotBlank @Size(max = 200) String title, String body) {
    }

    record NoteResponse(Ulid id, String title, String body, Instant createdAt) {

        static NoteResponse from(Note note) {
            return new NoteResponse(PublicId.of(note.getId()), note.getTitle(), note.getBody(), note.getCreatedAt());
        }
    }

    /** A page of notes; {@code nextCursor} is null on the last page. */
    record NotePage(List<NoteResponse> items, Ulid nextCursor) {
    }

    /** Both spellings of one identifier, plus what can be read out of it. */
    record IdInspection(Ulid ulid, UUID uuid, int uuidVersion, boolean issuedByThisService, Instant createdAt) {
    }
}
