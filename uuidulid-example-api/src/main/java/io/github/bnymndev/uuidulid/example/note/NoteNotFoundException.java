package io.github.bnymndev.uuidulid.example.note;

import io.github.bnymndev.uuidulid.Ulid;

public class NoteNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoteNotFoundException(Ulid id) {
        super("No note with id " + id);
    }
}
