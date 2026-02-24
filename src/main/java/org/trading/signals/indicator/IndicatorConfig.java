package org.trading.signals.indicator;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Configuration schema passed when instantiating indicators. Provides standard
 * parameters like lookback periods
 * and time constraints. Built using the Builder pattern.
 */
@Data
@Builder
public class IndicatorConfig {

    /**
     * Lookback window size for calculating averages or oscillators (default is 14
     * bars).
     */
    @Builder.Default
    private final int period = 14;

    /**
     * The maximum number of missing chronological periods before throwing an
     * adjacency data gap failure.
     */
    @Builder.Default
    private final int gapTolerance = 5;

    /**
     * The earliest timestamp allowed for this indicator to fetch data and begin
     * computing.
     */
    @Builder.Default
    private final LocalDateTime from = LocalDateTime.MIN;

    @Builder.Default
    private final LocalDateTime to = LocalDateTime.MAX;

    @Builder.Default
    private final Map<String, Object> metadata = Map.of();
}
