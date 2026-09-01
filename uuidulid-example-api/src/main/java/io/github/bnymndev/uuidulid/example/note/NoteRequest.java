package io.github.bnymndev.uuidulid.example.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 4000) String body) {
}
