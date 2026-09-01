package io.github.bnymndev.uuidulid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class UlidTest {

    /**
     * Vectors produced by an independent reference encoder, covering the boundaries of the value
     * space (zero, one, the randomness/timestamp split at bit 80, the top bit) and random values.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @CsvSource({
            "0, 0, 00000000000000000000000000, 0",
            "-1, -1, 7ZZZZZZZZZZZZZZZZZZZZZZZZZ, 281474976710655",
            "0, 1, 00000000000000000000000001, 0",
            "65535, -1, 0000000000ZZZZZZZZZZZZZZZZ, 0",
            "65536, 0, 00000000010000000000000000, 1",
            "-9223372036854775808, 0, 40000000000000000000000000, 140737488355328",
            "-3026442236816155972, 8498419270543363347, 6NZZND41ANKAY7BW3K9K6H4K8K, 235295133009237",
            "-8549444609539852218, -5049302510376531357, 49B92PSBBZQ13BKVA63T0R9JK3, 151020804812159",
            "5876994815293138050, 5369734896955510994, 2HHX3TB62W4J14N191E17GTM6J, 89675824207964",
            "7342118503010922499, 3189418111504105140, 35WHRY1NP9Y01JRGRPVZ46CFNM, 112031837509321",
            "6872782959255687761, 5081985638852049909, 2ZC43V066B298MD1PQ0DAPKEZN, 104870345447627",
            "-7725660435067830700, -2816585258571081570, 4MS3RC8G3XZSADHTBTM5BM9P4Y, 163590753763453",
            "-7039905993025341780, 6709313945628958214, 4Y9MXCRRCMPTP5T725FHQWKQG6, 174054536143252",
            "-5638731671164445685, 1818917104509493938, 5HQWSPQ73ZKG5HJFGQF0S2Q8NJ, 195434759560319",
            "-5398170775646655958, 22524129089560770, 5N2QC4FG5FN8N00M05JDWM4862, 199105427521711",
            "-269588126627005584, -8079239675697631361, 7W88XGDX5VZDR8ZR66A89CSVVZ, 277361388352699",
    })
    void encodesAndDecodesReferenceVectors(long msb, long lsb, String text, long timestamp) {
        Ulid ulid = Ulid.of(msb, lsb);

        assertThat(ulid).hasToString(text);
        assertThat(ulid.getTimestamp()).isEqualTo(timestamp);
        assertThat(Ulid.parse(text)).isEqualTo(ulid);
        assertThat(Ulid.parse(text).getMostSignificantBits()).isEqualTo(msb);
        assertThat(Ulid.parse(text).getLeastSignificantBits()).isEqualTo(lsb);
    }

    @Test
    void decodesTheSpecificationExample() {
        Ulid ulid = Ulid.parse("01ARZ3NDEKTSV4RRFFQ69G5FAV");

        assertThat(ulid.getTimestamp()).isEqualTo(1469922850259L);
        assertThat(ulid.getInstant()).isEqualTo(Instant.parse("2016-07-30T23:54:10.259Z"));
    }

    @Test
    void constantsAreTheBoundsOfTheValueSpace() {
        assertThat(Ulid.MIN).hasToString("00000000000000000000000000");
        assertThat(Ulid.MAX).hasToString("7ZZZZZZZZZZZZZZZZZZZZZZZZZ");
        assertThat(Ulid.MIN).isLessThan(Ulid.MAX);
        assertThat(Ulid.MAX.getTimestamp()).isEqualTo(Ulid.MAX_TIMESTAMP);
    }

    @Test
    void roundTripsThroughEveryRepresentation() {
        Random random = new Random(42);
        for (int i = 0; i < 10_000; i++) {
            Ulid ulid = Ulid.of(random.nextLong(), random.nextLong());

            assertThat(Ulid.parse(ulid.toString())).isEqualTo(ulid);
            assertThat(Ulid.fromBytes(ulid.toBytes())).isEqualTo(ulid);
            assertThat(Ulid.fromUuid(ulid.toUuid())).isEqualTo(ulid);
            assertThat(ulid.toBytes()).hasSize(Ulid.BYTES);
            assertThat(ulid.getRandomness()).hasSize(Ulid.RANDOMNESS_BYTES);
        }
    }

    /**
     * The property that makes ULIDs useful as sortable keys: the byte order, the text order and
     * the natural order all agree.
     */
    @Test
    void textOrderMatchesValueOrder() {
        Random random = new Random(7);
        List<Ulid> ulids = new ArrayList<>();
        for (int i = 0; i < 2_000; i++) {
            ulids.add(Ulid.of(random.nextLong(), random.nextLong()));
        }

        List<String> byValue = ulids.stream().sorted().map(Ulid::toString).collect(Collectors.toList());
        List<String> byText = ulids.stream().map(Ulid::toString).sorted().collect(Collectors.toList());

        assertThat(byValue).isEqualTo(byText);
    }

    @Test
    void comparesAsUnsigned() {
        // The most significant bit set means a "negative" long but a larger ULID.
        Ulid low = Ulid.of(0x7FFF_FFFF_FFFF_FFFFL, -1L);
        Ulid high = Ulid.of(0x8000_0000_0000_0000L, 0L);

        assertThat(low).isLessThan(high);
        assertThat(low.toString()).isLessThan(high.toString());
    }

    @Test
    void buildsFromTimestampAndRandomness() {
        byte[] randomness = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        Ulid ulid = Ulid.of(1_700_000_000_000L, randomness);

        assertThat(ulid.getTimestamp()).isEqualTo(1_700_000_000_000L);
        assertThat(ulid.getRandomness()).containsExactly(randomness);
    }

    @Test
    void buildsFromInstantAndRandomness() {
        Instant instant = Instant.parse("2026-09-01T12:00:00.123Z");
        Ulid ulid = Ulid.of(instant, new byte[Ulid.RANDOMNESS_BYTES]);

        assertThat(ulid.getInstant()).isEqualTo(instant);
        assertThat(ulid.getRandomness()).containsOnly((byte) 0);
    }

    @Test
    void rejectsRandomnessOfTheWrongLength() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Ulid.of(0L, new byte[9]))
                .withMessageContaining("10 bytes");
    }

    @Test
    void rejectsTimestampsOutsideTheValueSpace() {
        assertThatIllegalArgumentException().isThrownBy(() -> Ulid.of(-1L, new byte[10]));
        assertThatIllegalArgumentException().isThrownBy(() -> Ulid.of(Ulid.MAX_TIMESTAMP + 1, new byte[10]));
        assertThatIllegalArgumentException().isThrownBy(() -> Ulid.min(-1L));
        assertThatIllegalArgumentException().isThrownBy(() -> Ulid.max(Ulid.MAX_TIMESTAMP + 1));
    }

    @Test
    void bracketsATimeRange() {
        Instant instant = Instant.parse("2026-09-01T12:00:00.000Z");
        Ulid lower = Ulid.min(instant);
        Ulid upper = Ulid.max(instant);
        Ulid inside = Ulid.of(instant, new byte[]{5, 5, 5, 5, 5, 5, 5, 5, 5, 5});

        assertThat(lower).isLessThan(inside);
        assertThat(upper).isGreaterThan(inside);
        assertThat(lower.getTimestamp()).isEqualTo(upper.getTimestamp());
        assertThat(lower.getRandomness()).containsOnly((byte) 0);
        assertThat(upper.getRandomness()).containsOnly((byte) 0xFF);
        // Adjacent milliseconds do not overlap.
        assertThat(upper).isLessThan(Ulid.min(instant.plusMillis(1)));
    }

    @Test
    void incrementsToTheNextValue() {
        assertThat(Ulid.MIN.increment()).isEqualTo(Ulid.of(0L, 1L));
        // Carry across the 64-bit boundary.
        assertThat(Ulid.of(0L, -1L).increment()).isEqualTo(Ulid.of(1L, 0L));
        assertThat(Ulid.MIN.increment()).isGreaterThan(Ulid.MIN);

        assertThatExceptionOfType(ArithmeticException.class).isThrownBy(Ulid.MAX::increment);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "01arz3ndektsv4rrffq69g5fav",  // lowercase
            "01ARZ3NDEKTSV4RRFFQ69G5FAV",  // canonical
            "OIARZ3NDEKTSV4RRFFQ69G5FAV",  // Crockford aliases: O -> 0, I -> 1
            "0lARZ3NDEKTSV4RRFFQ69G5FAV",  // Crockford alias: l -> 1
    })
    void parsingIsLenientInTheWayCrockfordPrescribes(String text) {
        assertThat(Ulid.parse(text)).hasToString("01ARZ3NDEKTSV4RRFFQ69G5FAV");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "0123456789ABCDEFGHJKMNPQR",     // 25 characters
            "0123456789ABCDEFGHJKMNPQRST",   // 27 characters
            "U1ARZ3NDEKTSV4RRFFQ69G5FAV",    // U is not in the alphabet
            "01ARZ3NDEKTSV4RRFFQ69G5FA-",    // punctuation
            "01ARZ3NDEKTSV4RRFFQ69G5FAä", // non-ASCII
            "8ZZZZZZZZZZZZZZZZZZZZZZZZZ",    // overflows 128 bits
            "ZZZZZZZZZZZZZZZZZZZZZZZZZZ",    // overflows 128 bits
    })
    void rejectsInvalidText(String text) {
        assertThat(Ulid.isValid(text)).isFalse();
        assertThat(Ulid.tryParse(text)).isEmpty();
        assertThatIllegalArgumentException().isThrownBy(() -> Ulid.parse(text));
    }

    @Test
    void rejectsNullText() {
        assertThat(Ulid.isValid(null)).isFalse();
        assertThat(Ulid.tryParse(null)).isEmpty();
    }

    @Test
    void rejectsBinaryFormOfTheWrongLength() {
        assertThatIllegalArgumentException().isThrownBy(() -> Ulid.fromBytes(new byte[15]));
        assertThatIllegalArgumentException().isThrownBy(() -> Ulid.fromBytes(new byte[17]));
    }

    @Test
    void convertsToAndFromUuidWithoutLosingBits() {
        Ulid ulid = Ulid.parse("01ARZ3NDEKTSV4RRFFQ69G5FAV");
        UUID uuid = ulid.toUuid();

        assertThat(uuid.getMostSignificantBits()).isEqualTo(ulid.getMostSignificantBits());
        assertThat(uuid.getLeastSignificantBits()).isEqualTo(ulid.getLeastSignificantBits());
        assertThat(Ulid.fromUuid(uuid)).isEqualTo(ulid);
    }

    @Test
    void keepsTheTimestampWhenConvertedFromUuidV7() {
        UUID uuid = Uuid7Factory.monotonic().create();

        assertThat(Ulid.fromUuid(uuid).getTimestamp()).isEqualTo(Uuids.timestamp(uuid));
    }

    @Test
    void randomIdentifiersAreDistinctAndCarryTheCurrentTime() {
        long before = System.currentTimeMillis();
        Ulid ulid = Ulid.random();
        long after = System.currentTimeMillis();

        assertThat(ulid.getTimestamp()).isBetween(before, after);
        assertThat(Ulid.random()).isNotEqualTo(ulid);
    }

    @Test
    void honoursTheEqualsAndHashCodeContract() {
        Ulid a = Ulid.of(1L, 2L);
        Ulid b = Ulid.of(1L, 2L);
        Ulid c = Ulid.of(1L, 3L);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(c).isNotEqualTo(null).isNotEqualTo("string");
        assertThat(a.compareTo(b)).isZero();
    }
}
