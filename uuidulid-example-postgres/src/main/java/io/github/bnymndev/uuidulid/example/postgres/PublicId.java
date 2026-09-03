package io.github.bnymndev.uuidulid.example.postgres;

import io.github.bnymndev.uuidulid.Ulid;
import io.github.bnymndev.uuidulid.Uuids;

import java.util.UUID;

/**
 * Translates between the database identity (a UUIDv7) and the public identity (a ULID).
 *
 * <p>A ULID is the same 128 bits as the UUID written in 26 characters instead of 36, so the
 * translation is exact in both directions and preserves ordering: the ULID strings sort exactly
 * like the {@code uuid} column in PostgreSQL. That is what makes ULID cursors and time-range
 * queries against the primary key work without any extra column.
 */
final class PublicId {

    private PublicId() {
    }

    /** The public form of a stored identifier. */
    static Ulid of(UUID id) {
        return Ulid.fromUuid(id);
    }

    /**
     * The stored form of a public identifier.
     *
     * <p>Every 26-character string maps to some 128-bit value, but only UUIDv7 values can have
     * been issued by this service. Anything else is rejected up front rather than answered with a
     * misleading 404, which also stops callers from probing the key space with arbitrary values.
     *
     * @throws InvalidPublicIdException if the ULID does not encode a UUIDv7
     */
    static UUID toUuid(Ulid publicId) {
        UUID uuid = publicId.toUuid();
        if (!Uuids.isV7(uuid)) {
            throw new InvalidPublicIdException(publicId.toString());
        }
        return uuid;
    }
}
