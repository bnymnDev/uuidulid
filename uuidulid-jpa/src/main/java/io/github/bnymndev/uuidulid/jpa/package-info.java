/**
 * Jakarta Persistence attribute converters for {@link io.github.bnymndev.uuidulid.Ulid}.
 *
 * <p>{@link java.util.UUID} needs no converter: it is a basic type in Jakarta Persistence 3.1,
 * so a UUIDv7 from {@link io.github.bnymndev.uuidulid.Uuid7Factory} maps to a native
 * {@code uuid} column out of the box.
 */
package io.github.bnymndev.uuidulid.jpa;
