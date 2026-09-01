package io.github.bnymndev.uuidulid;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class UlidFactoryTest {

    private static final long FIXED_MILLIS = 1_700_000_000_000L;

    @Test
    void monotonicFactoryIsStrictlyIncreasingWithinOneMillisecond() {
        UlidFactory factory = UlidFactory.builder().clock(TestClock.at(FIXED_MILLIS)).build();

        List<Ulid> ulids = generate(factory, 10_000);

        assertThat(factory.isMonotonic()).isTrue();
        assertThat(ulids).isSorted().doesNotHaveDuplicates();
        for (int i = 1; i < ulids.size(); i++) {
            assertThat(ulids.get(i)).isGreaterThan(ulids.get(i - 1));
        }
        // 10 000 increments cannot exhaust 80 bits of randomness, so the timestamp is untouched.
        assertThat(ulids).allSatisfy(ulid -> assertThat(ulid.getTimestamp()).isEqualTo(FIXED_MILLIS));
    }

    @Test
    void monotonicFactoryStaysOrderedWhenTheClockJumpsBackwards() {
        TestClock clock = TestClock.at(FIXED_MILLIS);
        UlidFactory factory = UlidFactory.builder().clock(clock).build();

        Ulid before = factory.create();
        clock.advance(-5_000);
        Ulid after = factory.create();

        assertThat(after).isGreaterThan(before);
        assertThat(after.getTimestamp()).isEqualTo(FIXED_MILLIS);
    }

    @Test
    void monotonicFactoryFollowsTheClockForwards() {
        TestClock clock = TestClock.at(FIXED_MILLIS);
        UlidFactory factory = UlidFactory.builder().clock(clock).build();

        Ulid first = factory.create();
        clock.advance(1);
        Ulid second = factory.create();

        assertThat(first.getTimestamp()).isEqualTo(FIXED_MILLIS);
        assertThat(second.getTimestamp()).isEqualTo(FIXED_MILLIS + 1);
        assertThat(second).isGreaterThan(first);
    }

    /**
     * Exhausting the 80 random bits within one millisecond must not break ordering: the carry
     * moves the identifier into the following millisecond.
     */
    @Test
    void randomnessOverflowCarriesIntoTheTimestamp() {
        LongSupplier allOnes = () -> -1L;
        UlidFactory factory = UlidFactory.builder()
                .clock(TestClock.at(FIXED_MILLIS))
                .randomSource(allOnes)
                .build();

        Ulid first = factory.create();
        Ulid second = factory.create();

        assertThat(first.getTimestamp()).isEqualTo(FIXED_MILLIS);
        assertThat(first.getRandomness()).containsOnly((byte) 0xFF);
        assertThat(second.getTimestamp()).isEqualTo(FIXED_MILLIS + 1);
        assertThat(second.getRandomness()).containsOnly((byte) 0x00);
        assertThat(second).isGreaterThan(first);
    }

    @Test
    void randomFactoryDrawsFreshRandomnessAndStillCarriesTheClock() {
        UlidFactory factory = UlidFactory.builder()
                .clock(TestClock.at(FIXED_MILLIS))
                .monotonic(false)
                .build();

        List<Ulid> ulids = generate(factory, 1_000);

        assertThat(factory.isMonotonic()).isFalse();
        assertThat(ulids).doesNotHaveDuplicates();
        assertThat(ulids).allSatisfy(ulid -> assertThat(ulid.getTimestamp()).isEqualTo(FIXED_MILLIS));
    }

    @Test
    void factoryIsUsableAsASupplier() {
        UlidFactory factory = UlidFactory.monotonic();

        assertThat(factory.get()).isNotNull().isLessThan(factory.get());
    }

    @Test
    void monotonicFactoryIsSafeUnderConcurrentUse() throws Exception {
        UlidFactory factory = UlidFactory.monotonic();
        int threads = 8;
        int perThread = 5_000;

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ConcurrentLinkedQueue<List<Ulid>> results = new ConcurrentLinkedQueue<>();

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

        Set<Ulid> all = results.stream().flatMap(List::stream).collect(Collectors.toSet());
        assertThat(all).hasSize(threads * perThread);
        // Each thread observes its own identifiers in increasing order.
        assertThat(results).allSatisfy(perThreadResult -> assertThat(perThreadResult).isSorted());
    }

    private static List<Ulid> generate(UlidFactory factory, int count) {
        List<Ulid> ulids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ulids.add(factory.create());
        }
        return ulids;
    }
}
