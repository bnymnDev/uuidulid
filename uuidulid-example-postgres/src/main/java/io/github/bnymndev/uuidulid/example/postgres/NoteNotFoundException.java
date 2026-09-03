package io.github.bnymndev.uuidulid.example.postgres;

import io.github.bnymndev.uuidulid.Ulid;

class NoteNotFoundException extends RuntimeException {

    NoteNotFoundException(Ulid id) {
        super("Note " + id + " does not exist");
    }
}
