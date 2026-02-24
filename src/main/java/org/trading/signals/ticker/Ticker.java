package org.trading.signals.ticker;

import org.trading.Timeline;
import org.trading.etoro.CandlesEndpoint;
import org.trading.etoro.WsEndpoint;
import org.trading.signals.Signal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A Signal implementation that acts as an entry point for external market data
 * (tickers/assets).
 * It fetches historical data via HTTP for backtesting or streams real-time data
 * via WebSockets for live trading.
 */
public class Ticker extends Signal {

    private final LocalDateTime from;
    private final LocalDateTime to;
    private final CandlesEndpoint candlesEndpoint;
    private final WsEndpoint wsEndpoint;

    /**
     * Constructs a Ticker signal based on the provided configuration.
     *
     * @param config The setting object holding the ticker ID, time boundaries, and
     *               endpoint references.
     */
    public Ticker(TickerConfig config) {
        super(config.getId(), config.getMetadata(), List.of());
        this.from = config.getFrom();
        this.to = config.getTo();
        this.candlesEndpoint = config.getCandlesEndpoint();
        this.wsEndpoint = config.getWsEndpoint();
    }

    /**
     * Helper method to instantiate default Tickers for multiple IDs simultaneously.
     *
     * @param ids A variable list of ticker symbols (e.g., "BTC", "AAPL").
     * @return A list of initialized Ticker signals.
     */
    public static List<Signal> defaultTickers(String... ids) {
        return Stream.of(ids)
                .map(Ticker::defaultOf)
                .map(Signal.class::cast)
                .toList();
    }

    /**
     * Helper method to instantiate a single Ticker with default configuration
     * values.
     *
     * @param id The symbol of the ticker.
     * @return An initialized Ticker signal.
     */
    public static Ticker defaultOf(String id) {
        return new Ticker(TickerConfig.builder().id(id).build());
    }

    /**
     * Queries the external {@link CandlesEndpoint} for historical candles, sets
     * this instance as their parent signal,
     * and seeds them into the Timeline's core data structure.
     */
    @Override
    public void onBacktesting(Timeline timeline) {
        var freq = timeline.getFrequency();

        var candles = candlesEndpoint.fetch(id, freq, 1000);
        var events = candles.stream()
                .filter(c -> c.getTime().isAfter(this.from) && c.getTime().isBefore(this.to))
                .peek(c -> c.setSignal(this))
                .map(Signal.Event.class::cast)
                .toList();

        timeline.addEvents(events);
    }

    /**
     * Initiates a live WebSocket subscription to standard external streams, mapping
     * incoming
     * json data to Candle events which are funneled out to the consumer.
     */
    @Override
    public void onLiveUpdate(Consumer<List<Signal.Event>> subscriber) {

        wsEndpoint.subscribe(getId(), events -> {
            subscriber.accept(events.stream().map(Signal.Event.class::cast).toList());
        });
    }
}
