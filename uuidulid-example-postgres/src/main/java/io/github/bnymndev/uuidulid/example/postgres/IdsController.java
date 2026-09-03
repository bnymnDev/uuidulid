package io.github.bnymndev.uuidulid.example.postgres;

import io.github.bnymndev.uuidulid.Ulid;
import io.github.bnymndev.uuidulid.Uuids;
import io.github.bnymndev.uuidulid.example.postgres.NoteDtos.IdInspection;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/** Shows that a ULID and a UUID are two spellings of the same value. */
@RestController
@RequestMapping("/api/ids")
class IdsController {

    /** Accepts either spelling and returns both. */
    @GetMapping("/inspect/{value}")
    IdInspection inspect(@PathVariable String value) {
        Optional<UUID> fromUlid = Ulid.tryParse(value).map(Ulid::toUuid);
        UUID uuid = fromUlid.or(() -> Uuids.tryParse(value))
                .orElseThrow(() -> new InvalidPublicIdException(value));
        boolean issuedHere = Uuids.isV7(uuid);
        return new IdInspection(Ulid.fromUuid(uuid), uuid, uuid.version(), issuedHere,
                issuedHere ? Uuids.instant(uuid) : null);
    }
}
