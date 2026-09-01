package io.github.bnymndev.uuidulid;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class Uuid7FactoryTest {

    private static final long FIXED_MILLIS = 1_700_000_000_000L;

    @Test
    void producesTheRfc9562Layout() {
        UUID uuid = Uuid7Factory.builder().clock(TestClock.at(FIXED_MILLIS)).build().create();

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
        assertThat(uuid.getMostSignificantBits() >>> 16).isEqualTo(FIXED_MILLIS);
        assertThat(Uuids.timestamp(uuid)).isEqualTo(FIXED_MILLIS);
        assertThat(Uuids.isV7(uuid)).isTrue();
    }

    @Test
    void monotonicFactoryIsStrictlyIncreasingWithinOneMillisecond() {
        Uuid7Factory factory = Uuid7Factory.builder().clock(TestClock.at(FIXED_MILLIS)).build();

        List<UUID> uuids = generate(factory, 10_000);

        assertThat(factory.isMonotonic()).isTrue();
        for (int i = 1; i < uuids.size(); i++) {
            assertThat(Uuids.compareUnsigned(uuids.get(i), uuids.get(i - 1))).isPositive();
        }
        assertThat(uuids).allSatisfy(uuid -> {
            assertThat(uuid.version()).isEqualTo(7);
            assertThat(uuid.variant()).isEqualTo(2);
            assertThat(Uuids.timestamp(uuid)).isEqualTo(FIXED_MILLIS);
        });
    }

    @Test
    void monotonicFactoryStaysOrderedWhenTheClockJumpsBackwards() {
        TestClock clock = TestClock.at(FIXED_MILLIS);
        Uuid7Factory factory = Uuid7Factory.builder().clock(clock).build();

        UUID before = factory.create();
        clock.advance(-5_000);
        UUID after = factory.create();

        assertThat(Uuids.compareUnsigned(after, before)).isPositive();
        assertThat(Uuids.timestamp(after)).isEqualTo(FIXED_MILLIS);
    }

    /**
     * When both counter fields are exhausted within a millisecond the factory moves on to the
     * next millisecond rather than emitting a duplicate or corrupting the version bits.
     */
    @Test
    void counterOverflowMovesToTheNextMillisecond() {
        LongSupplier allOnes = () -> -1L;
        Uuid7Factory factory = Uuid7Factory.builder()
                .clock(TestClock.at(FIXED_MILLIS))
                .randomSource(allOnes)
                .build();

        UUID first = factory.create();
        UUID second = factory.create();

        assertThat(Uuids.timestamp(first)).isEqualTo(FIXED_MILLIS);
        assertThat(Uuids.timestamp(second)).isEqualTo(FIXED_MILLIS + 1);
        assertThat(second.version()).isEqualTo(7);
        assertThat(second.variant()).isEqualTo(2);
        assertThat(Uuids.compareUnsigned(second, first)).isPositive();
    }

    @Test
    void counterCarriesFromRandBIntoRandA() {
        // rand_b starts at its maximum, rand_a at zero: the next value must carry into rand_a
        // without disturbing the timestamp, version or variant.
        LongSupplier randA0RandBMax = new LongSupplier() {
            private int call;

            @Override
            public long getAsLong() {
                return call++ == 0 ? 0L : -1L;
            }
        };
        Uuid7Factory factory = Uuid7Factory.builder()
                .clock(TestClock.at(FIXED_MILLIS))
                .randomSource(randA0RandBMax)
                .build();

        UUID first = factory.create();
        UUID second = factory.create();

        assertThat(first.getMostSignificantBits() & 0x0FFFL).isZero();
        assertThat(second.getMostSignificantBits() & 0x0FFFL).isEqualTo(1L);
        assertThat(second.getLeastSignificantBits()).isEqualTo(0x8000_0000_0000_0000L);
        assertThat(Uuids.timestamp(second)).isEqualTo(FIXED_MILLIS);
        assertThat(second.version()).isEqualTo(7);
        assertThat(second.variant()).isEqualTo(2);
        assertThat(Uuids.compareUnsigned(second, first)).isPositive();
    }

    @Test
    void randomFactoryProducesDistinctIdentifiers() {
        Uuid7Factory factory = Uuid7Factory.builder()
                .clock(TestClock.at(FIXED_MILLIS))
                .monotonic(false)
                .build();

        List<UUID> uuids = generate(factory, 1_000);

        assertThat(factory.isMonotonic()).isFalse();
        assertThat(uuids).doesNotHaveDuplicates();
        assertThat(uuids).allSatisfy(uuid -> assertThat(uuid.version()).isEqualTo(7));
    }

    @Test
    void factoryIsUsableAsASupplier() {
        Uuid7Factory factory = Uuid7Factory.monotonic();

        assertThat(factory.get()).isNotNull();
    }

    @Test
    void monotonicFactoryIsSafeUnderConcurrentUse() throws Exception {
        Uuid7Factory factory = Uuid7Factory.monotonic();
        int threads = 8;
        int perThread = 5_000;

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ConcurrentLinkedQueue<List<UUID>> results = new ConcurrentLinkedQueue<>();

        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    start.await();
                    results.add(generate(factory, perThread));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();

        Set<UUID> all = results.stream().flatMap(List::stream).collect(Collectors.toSet());
        assertThat(all).hasSize(threads * perThread);
        assertThat(all).allSatisfy(uuid -> {
            assertThat(uuid.version()).isEqualTo(7);
            assertThat(uuid.variant()).isEqualTo(2);
        });
    }

    private static List<UUID> generate(Uuid7Factory factory, int count) {
        List<UUID> uuids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            uuids.add(factory.create());
        }
        return uuids;
    }
}
