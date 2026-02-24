package org.trading.signals.ticker;

import lombok.Builder;
import lombok.Data;
import org.trading.etoro.CandlesEndpoint;
import org.trading.etoro.WsEndpoint;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Structural definition for orchestrating a Ticker instance.
 */
@Data
@Builder
public class TickerConfig {

    /**
     * The unique asset symbol identifier for this ticker.
     */
    private final String id;

    /**
     * Start time boundary for fetching historical data.
     */
    @Builder.Default
    private final LocalDateTime from = LocalDateTime.MIN;

    /**
     * End time boundary for fetching historical data.
     */
    @Builder.Default
    private final LocalDateTime to = LocalDateTime.MAX;

    /**
     * Reference to the API used to historically seed backtests. Defaults to eToro.
     */
    @Builder.Default
    private final CandlesEndpoint candlesEndpoint = CandlesEndpoint.ETORO();

    /**
     * Reference to the active stream API used in live modes. Defaults to eToro.
     */
    @Builder.Default
    private final WsEndpoint wsEndpoint = WsEndpoint.ETORO();

    @Builder.Default
    private final Map<String, Object> metadata = Map.of();
}
