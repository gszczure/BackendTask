package org.codibly.time;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public interface TimeProvider {

    ZonedDateTime getZonedDateTime();

    LocalDate getLocalDate();

    ZonedDateTime getStartOfDay();

    ZonedDateTime getEndOfDay();
}
