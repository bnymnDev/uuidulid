package io.github.bnymndev.uuidulid.example.ids;

import io.github.bnymndev.uuidulid.Ulid;
import io.github.bnymndev.uuidulid.UlidFactory;
import io.github.bnymndev.uuidulid.Uuid7Factory;
import io.github.bnymndev.uuidulid.Uuids;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/** Utility endpoints: mint identifiers of each kind, and take an identifier apart. */
@RestController
@RequestMapping("/api/ids")
class IdController {

    private static final int MAX_COUNT = 1_000;

    private final UlidFactory ulids;
    private final Uuid7Factory uuid7s;

    IdController(UlidFactory ulids, Uuid7Factory uuid7s) {
        this.ulids = ulids;
        this.uuid7s = uuid7s;
    }

    @GetMapping("/ulid")
    List<Ulid> ulid(@RequestParam(defaultValue = "1") int count) {
        return Stream.generate(ulids::create).limit(clamp(count)).toList();
    }

    @GetMapping("/uuid7")
    List<UUID> uuid7(@RequestParam(defaultValue = "1") int count) {
        return Stream.generate(uuid7s::create).limit(clamp(count)).toList();
    }

    @GetMapping("/uuid4")
    List<UUID> uuid4(@RequestParam(defaultValue = "1") int count) {
        return Stream.generate(Uuids::v4).limit(clamp(count)).toList();
    }

    /** Accepts a ULID or any textual UUID form and reports what it carries. */
    @GetMapping("/inspect/{value}")
    IdInfo inspect(@PathVariable String value) {
        Optional<Ulid> ulid = Ulid.tryParse(value);
        if (ulid.isPresent()) {
            return IdInfo.ofUlid(ulid.get());
        }
        return Uuids.tryParse(value)
                .map(IdInfo::ofUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "'" + value + "' is neither a ULID nor a UUID"));
    }

    private static int clamp(int count) {
        return Math.max(1, Math.min(count, MAX_COUNT));
    }

    /**
     * Everything that can be learnt from an identifier. {@code timestamp}/{@code instant} are
     * {@code null} for UUID versions that do not embed a time.
     */
    record IdInfo(String kind, Ulid ulid, UUID uuid, Integer uuidVersion, Long timestamp, Instant instant) {

        static IdInfo ofUlid(Ulid ulid) {
            return new IdInfo("ULID", ulid, ulid.toUuid(), null, ulid.getTimestamp(), ulid.getInstant());
        }

        static IdInfo ofUuid(UUID uuid) {
            int version = uuid.version();
            boolean timeBased = version == 1 || version == 6 || version == 7;
            return new IdInfo("UUID", Ulid.fromUuid(uuid), uuid, version,
                    timeBased ? Uuids.timestamp(uuid) : null,
                    timeBased ? Uuids.instant(uuid) : null);
        }
    }
}
