package io.github.bnymndev.uuidulid.example.note;

import io.github.bnymndev.uuidulid.Ulid;

import java.time.Instant;

/** What the API returns for a note. The {@code id} serialises as a 26-character ULID string. */
public record NoteResponse(Ulid id, String title, String body, Instant createdAt) {

    static NoteResponse from(Note note) {
        return new NoteResponse(note.getId(), note.getTitle(), note.getBody(), note.getCreatedAt());
    }
}
