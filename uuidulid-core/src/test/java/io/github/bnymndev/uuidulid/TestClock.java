package io.github.bnymndev.uuidulid;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** A clock whose time only moves when a test tells it to. */
final class TestClock extends Clock {

    private long millis;

    private TestClock(long millis) {
        this.millis = millis;
    }

    static TestClock at(long millis) {
        return new TestClock(millis);
    }

    void set(long millis) {
        this.millis = millis;
    }

    void advance(long delta) {
        this.millis += delta;
    }

    @Override
    public long millis() {
        return millis;
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(millis);
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
