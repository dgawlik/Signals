package org.dgawlik.signals.signals.indicator.library;

import org.dgawlik.signals.Timeline;
import org.dgawlik.signals.signals.indicator.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Exponential Moving Average (EMA).
 * An indicator that tracks the average price over a specific period, applying
 * more weight to recent prices
 * to make it more responsive to new information than a simple moving average.
 */
public class EMA extends BaseIndicator {

    private final int period;
    private final String forId;
    private Timeline.EventsAtTick previousEvent;
    private LinkedList<Double> prices = new LinkedList<>();
    private AdjacencyChecker adjacencyChecker;

    /**
     * Constructs an EMA indicator.
     *
     * @param forId  The identifier of the prerequisite signal (e.g., Ticker
     *               symbol).
     * @param config The structural configuration containing the period size.
     */
    public EMA(String forId,
            IndicatorConfig config) {
        super("EMA." + forId, List.of(forId), config);
        this.period = config.getPeriod();
        this.adjacencyChecker = new AdjacencyChecker(forId, config.getGapTolerance());
        this.forId = forId;
    }

    @Override
    public CalculationResult onTick(Timeline.EventsAtTick tick) {
        if (!adjacencyChecker.isContingent(tick)) {
            return new CalculationResult.Failure("Data is not contiguous for id: " + forId);
        }

        var currentEvent = tick.forId(forId);

        if (currentEvent == null) {
            return new CalculationResult.Waiting();
        }

        var price = currentEvent.getValue("close");
        if (Double.isNaN(price)) {
            return new CalculationResult.Waiting();
        }

        prices.offerLast(price);

        if (prices.size() < period - 1) {
            return new CalculationResult.Waiting();
        } else {
            var ema = prices.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
            var ema2 = ((period - 1) * ema + price) / period;
            prices.removeFirst();
            var iv = new IndicatorValue(tick.getTime(), ema2, null);
            iv.setSignal(this);
            return new CalculationResult.Success(iv);
        }
    }
}
