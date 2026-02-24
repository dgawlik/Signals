package org.trading.signals.indicator;

import org.trading.Timeline;
import org.trading.signals.Signal;
import org.trading.signals.indicator.CalculationResult.Failure;
import org.trading.signals.indicator.CalculationResult.Success;
import org.trading.signals.indicator.CalculationResult.Waiting;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * Abstract class representing a trading indicator.
 * Indicators subscribe to one or more upstream Signals (prerequisites) and
 * compute streaming values
 * based on them (e.g., Simple Moving Average, RSI).
 */
public abstract class BaseIndicator extends Signal {

    private final LocalDateTime from;
    private final LocalDateTime to;

    public BaseIndicator(String id,
            List<String> prerequiredIds,
            IndicatorConfig config

    ) {
        super(id, config.getMetadata(), prerequiredIds);
        this.from = config.getFrom();
        this.to = config.getTo();
    }

    /**
     * Subscribes to necessary pre-required signals via the timeline, runs the
     * computation strategy on historical data,
     * and writes newly generated {@link IndicatorValue} events into the Timeline.
     */
    @Override
    public void onBacktesting(Timeline timeline) {
        var events = timeline.getEvents(from, to, prerequiredIds.toArray(new String[0]));

        events.forEach(ev -> {
            switch (onTick(ev)) {
                case Success(var indicatorValue) -> {
                    indicatorValue.setSignal(this);
                    timeline.addEvents(List.of(indicatorValue));
                }
                case Waiting() -> {
                    // do nothing, wait for more data
                }
                case Failure(String errorMessage) -> {
                    // TODO implment logging
                }
            }
        });
    }

    /**
     * Throws an {@link IllegalStateException} since indicators generally process
     * data through the timeline loop directly.
     */
    @Override
    public void onLiveUpdate(Consumer<List<Signal.Event>> subscriber) {
        throw new IllegalStateException("Not implemented");
    }

    /**
     * Strategy pattern method hook that subclasses implement to execute their
     * single-tick calculations.
     * Evaluated chronologically for each discrete timeline tick containing required
     * upstream data.
     *
     * @param tick A group of events arriving at the current time step.
     * @return The outcome of the calculation, dictating if enough data was gathered
     *         to emit a new indicator value.
     */
    public abstract CalculationResult onTick(Timeline.EventsAtTick tick);
}
