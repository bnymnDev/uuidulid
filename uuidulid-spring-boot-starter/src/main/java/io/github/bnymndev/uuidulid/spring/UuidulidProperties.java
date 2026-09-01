package io.github.bnymndev.uuidulid.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings for the auto-configured identifier factories, bound from the {@code uuidulid.*} prefix. */
@ConfigurationProperties(prefix = "uuidulid")
public class UuidulidProperties {

    /**
     * Whether the auto-configured UlidFactory and Uuid7Factory guarantee strictly increasing
     * identifiers within one millisecond. Monotonic identifiers make consecutive values from the
     * same millisecond guessable from one another; disable this when identifiers are handed out
     * publicly and must not be enumerable.
     */
    private boolean monotonic = true;

    public boolean isMonotonic() {
        return monotonic;
    }

    public void setMonotonic(boolean monotonic) {
        this.monotonic = monotonic;
    }
}
