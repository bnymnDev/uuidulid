package io.github.bnymndev.uuidulid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class UuidsTest {

    /** RFC 9562 Appendix A example UUIDs; all three encode 2022-02-22T19:22:22Z. */
    private static final UUID RFC_V1 = UUID.fromString("c232ab00-9414-11ec-b3c8-9e6bdeced846");
    private static final UUID RFC_V6 = UUID.fromString("1ec9414c-232a-6b00-b3c8-9e6bdeced846");
    private static final UUID RFC_V7 = UUID.fromString("017f22e2-79b0-7cc3-98c4-dc0c0c07398f");
    private static final long RFC_MILLIS = 1_645_557_742_000L;

    @Test
    void generatesVersion4Uuids() {
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            uuids.add(Uuids.v4());
        }

        assertThat(uuids).doesNotHaveDuplicates();
        assertThat(uuids).allSatisfy(uuid -> {
            assertThat(uuid.version()).isEqualTo(4);
            assertThat(uuid.variant()).isEqualTo(2);
        });
    }

    @Test
    void generatesVersion7Uuids() {
        long before = System.currentTimeMillis();
        UUID uuid = Uuids.v7();
        long after = System.currentTimeMillis();

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
        assertThat(Uuids.timestamp(uuid)).isBetween(before, after);
        assertThat(Uuids.v7()).isNotEqualTo(uuid);
    }

    @Test
    void readsTheTimestampOfEveryTimeBasedVersion() {
        assertThat(Uuids.timestamp(RFC_V1)).isEqualTo(RFC_MILLIS);
        assertThat(Uuids.timestamp(RFC_V6)).isEqualTo(RFC_MILLIS);
        assertThat(Uuids.timestamp(RFC_V7)).isEqualTo(RFC_MILLIS);
        assertThat(Uuids.instant(RFC_V7)).isEqualTo(Instant.parse("2022-02-22T19:22:22Z"));
    }

    @Test
    void refusesToInventATimestampForVersionsThatHaveNone() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Uuids.timestamp(Uuids.v4()))
                .withMessageContaining("version 4");
        assertThatIllegalArgumentException().isThrownBy(() -> Uuids.timestamp(Uuids.NIL));
    }

    @Test
    void recognisesVersion7() {
        assertThat(Uuids.isV7(RFC_V7)).isTrue();
        assertThat(Uuids.isV7(RFC_V1)).isFalse();
        assertThat(Uuids.isV7(Uuids.v4())).isFalse();
        assertThat(Uuids.isV7(null)).isFalse();
    }

    @Test
    void exposesTheSpecialUuids() {
        assertThat(Uuids.NIL).hasToString("00000000-0000-0000-0000-000000000000");
        assertThat(Uuids.MAX).hasToString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "017f22e2-79b0-7cc3-98c4-dc0c0c07398f",
            "017F22E2-79B0-7CC3-98C4-DC0C0C07398F",
            "017f22e279b07cc398c4dc0c0c07398f",
            "{017f22e2-79b0-7cc3-98c4-dc0c0c07398f}",
            "urn:uuid:017f22e2-79b0-7cc3-98c4-dc0c0c07398f",
            "URN:UUID:017f22e2-79b0-7cc3-98c4-dc0c0c07398f",
    })
    void parsesTheCommonTextualForms(String text) {
        assertThat(Uuids.parse(text)).isEqualTo(RFC_V7);
        assertThat(Uuids.isValid(text)).isTrue();
        assertThat(Uuids.tryParse(text)).contains(RFC_V7);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "not-a-uuid",
            "017f22e2-79b0-7cc3-98c4-dc0c0c07398",    // 31 digits
            "017f22e2-79b0-7cc3-98c4-dc0c0c07398ff",  // 33 digits
            "017f22e2-79b0-7cc3-98c4-dc0c0c07398g",   // g is not hexadecimal
            "1-1-1-1-1",                              // accepted by UUID.fromString, but not a UUID
    })
    void rejectsMalformedText(String text) {
        assertThat(Uuids.isValid(text)).isFalse();
        assertThat(Uuids.tryParse(text)).isEmpty();
        assertThatIllegalArgumentException().isThrownBy(() -> Uuids.parse(text));
    }

    @Test
    void rejectsNullText() {
        assertThat(Uuids.isValid(null)).isFalse();
        assertThat(Uuids.tryParse(null)).isEmpty();
    }

    @Test
    void roundTripsThroughTheBinaryForm() {
        Random random = new Random(1234);
        for (int i = 0; i < 1_000; i++) {
            UUID uuid = new UUID(random.nextLong(), random.nextLong());

            assertThat(Uuids.toBytes(uuid)).hasSize(16);
            assertThat(Uuids.fromBytes(Uuids.toBytes(uuid))).isEqualTo(uuid);
        }
        assertThat(Uuids.toBytes(Uuids.NIL)).containsOnly((byte) 0);
        assertThat(Uuids.toBytes(Uuids.MAX)).containsOnly((byte) 0xFF);
        assertThatIllegalArgumentException().isThrownBy(() -> Uuids.fromBytes(new byte[15]));
    }

    @Test
    void roundTripsThroughUlid() {
        Ulid ulid = Ulid.random();

        assertThat(Uuids.toUlid(Uuids.fromUlid(ulid))).isEqualTo(ulid);
        assertThat(Uuids.fromUlid(ulid)).isEqualTo(ulid.toUuid());
    }

    /**
     * {@link UUID#compareTo(UUID)} compares the halves as signed longs, so it disagrees with the
     * ordering of the textual and binary forms. {@link Uuids#compareUnsigned} does not.
     */
    @Test
    void unsignedComparisonMatchesTextualOrdering() {
        UUID low = UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff");
        UUID high = UUID.fromString("80000000-0000-0000-0000-000000000000");

        assertThat(Uuids.compareUnsigned(low, high)).isNegative();
        assertThat(low.toString()).isLessThan(high.toString());
        assertThat(low.compareTo(high)).isPositive();  // the JDK gets this "wrong" for sorting

        Random random = new Random(99);
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            uuids.add(new UUID(random.nextLong(), random.nextLong()));
        }
        List<String> byValue = uuids.stream().sorted(Uuids.unsignedComparator()).map(UUID::toString).collect(Collectors.toList());
        List<String> byText = uuids.stream().map(UUID::toString).sorted().collect(Collectors.toList());
        assertThat(byValue).isEqualTo(byText);
    }
}
