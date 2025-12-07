package org.codibly.time;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZonedDateTime;

@Component
public class SystemTimeProvider implements TimeProvider {

    private final Clock clock;

    public SystemTimeProvider() {
        this.clock = Clock.systemUTC();
    }

    public SystemTimeProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public ZonedDateTime get() {
        return ZonedDateTime.now(clock);
    }
}
