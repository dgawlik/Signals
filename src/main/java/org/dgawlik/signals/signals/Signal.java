package org.dgawlik.signals.signals;

import org.dgawlik.signals.Timeline;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The base unit of the trading library. A signal generates discrete
 * {@link Event}s over time,
 * responding to either backtest intervals or live streaming ticks.
 * Subclasses like Tickers emit raw market data, whereas Indicators emit
 * computed statistics.
 */
public abstract class Signal {
    /**
     * Represents a single observation, data point, or calculation emitted by a
     * Signal at a specific point in time.
     */
    public interface Event {

        /**
         * @return The unique identifier of the signal that emitted this event.
         */
        String getId();

        /**
         * @return The timestamp indicating when this event occurred or was computed.
         */
        LocalDateTime getTime();

        /**
         * @return The parent {@link Signal} instance that generated the event.
         */
        Signal getSignal();

        /**
         * @return Key-value pairs of metadata associated with the event.
         */
        Map<String, Object> getMetadata();

        /**
         * @return A list of the available metric names (keys) stored inside this event.
         */
        List<String> getKeys();

        /**
         * Retrieves the numerical value for a specific metric key.
         *
         * @param key The metric name.
         * @return The value associated with the given key.
         */
        double getValue(String key);

    }

    protected final String id;
    protected final Map<String, Object> metadata;
    protected final List<String> prerequiredIds;

    public Signal(
            String id,
            Map<String, Object> metadata,
            List<String> prerequiredIds) {
        this.id = id;
        this.metadata = metadata;
        this.prerequiredIds = prerequiredIds;
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Connects this signal to an offline timeline. The signal responds by querying
     * historical events
     * from the timeline, calculating its outputs, and adding its generated events
     * back into the timeline.
     *
     * @param timeline The backtesting timeline framework.
     */
    public abstract void onBacktesting(Timeline timeline);

    /**
     * Connects this signal to live data feeds. As live real-time packets arrive,
     * they form events and
     * are pushed to the provided subscriber callback.
     *
     * @param subscriber The consumer callback to receive live stream events.
     */
    public abstract void onLiveUpdate(Consumer<List<Signal.Event>> subscriber);
}
