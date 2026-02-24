package org.dgawlik.signals.signals.indicator;

import org.dgawlik.signals.Timeline;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Validates contiguous data streams. Checks incoming chronological event ticks
 * against previous ones to ensure
 * they don't surpass standard duration gaps according to the timeline
 * Frequency. Prevents indicators
 * from hallucinating computations over massive holes in historical data.
 */
public class AdjacencyChecker {

    private Timeline.EventsAtTick previousEvent;
    private final String forId;
    private final int gapTolerance;
    private int missingDataCount;

    public AdjacencyChecker(String forId, int gapTolerance) {
        this.forId = forId;
        this.gapTolerance = gapTolerance;
    }

    /**
     * Validates whether a given tick forms a continuous sequence since the
     * previously tracked one.
     *
     * @param tick The newly ingested timeline tick containing a snapshot of active
     *             events.
     * @return True if the transition conforms within gap constraints; False if it
     *         violates maximum allowed continuity gaps.
     */
    public boolean isContingent(Timeline.EventsAtTick tick) {
        if (previousEvent == null) {
            previousEvent = tick;
            return true;
        }

        if (tick.forId(forId) == null) {
            missingDataCount++;
        } else {
            missingDataCount = 0;
        }

        if (missingDataCount > gapTolerance) {
            return false;
        }

        Duration d = Duration.between(previousEvent.getTime(), tick.getTime());

        var allowedDuration = switch (tick.getFrequency()) {
            case TICK -> Duration.of(gapTolerance, ChronoUnit.SECONDS);
            case ONE_MINUTE -> Duration.of(gapTolerance, ChronoUnit.MINUTES);
            case FIVE_MINUTES -> Duration.of(gapTolerance * 5L, ChronoUnit.MINUTES);
            case TEN_MINUTES -> Duration.of(gapTolerance * 10L, ChronoUnit.MINUTES);
            case FIFTEEN_MINUTES -> Duration.of(gapTolerance * 15L, ChronoUnit.MINUTES);
            case THIRTY_MINUTES -> Duration.of(gapTolerance * 30L, ChronoUnit.MINUTES);
            case ONE_HOUR -> Duration.of(gapTolerance, ChronoUnit.HOURS);
            case FOUR_HOURS -> Duration.of(gapTolerance * 4L, ChronoUnit.HOURS);
            case ONE_DAY -> Duration.of(gapTolerance, ChronoUnit.DAYS);
        };

        if (d.compareTo(allowedDuration) > 0) {
            return false;
        }

        previousEvent = tick;

        return true;
    }
}
