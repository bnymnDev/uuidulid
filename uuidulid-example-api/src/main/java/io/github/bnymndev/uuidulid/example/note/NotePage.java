package io.github.bnymndev.uuidulid.example.note;

import io.github.bnymndev.uuidulid.Ulid;

import java.util.List;

/**
 * A page of notes with a keyset cursor: pass {@code nextCursor} as {@code ?after=} to fetch the
 * following page. {@code null} means there is no further page.
 */
public record NotePage(List<NoteResponse> items, Ulid nextCursor) {
}
