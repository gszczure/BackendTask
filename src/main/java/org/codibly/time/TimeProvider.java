package org.codibly.time;

import java.time.ZonedDateTime;
import java.util.function.Supplier;

@FunctionalInterface
public interface TimeProvider extends Supplier<ZonedDateTime> {
}
