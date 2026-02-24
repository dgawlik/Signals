package org.dgawlik.signals.signals.indicator.library;

import org.dgawlik.signals.Timeline;
import org.dgawlik.signals.signals.indicator.*;
import org.dgawlik.signals.signals.indicator.*;
import org.dgawlik.signals.signals.indicator.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Average True Range (ATR).
 * A volatility indicator that measures market volatility by decomposing the
 * entire range of an asset price for that period.
 */
public class ATR extends BaseIndicator {

    private final int period;
    private final String forId;
    private Timeline.EventsAtTick previousEvent;
    private LinkedList<Double> trValues = new LinkedList<>();
    private AdjacencyChecker adjacencyChecker;

    /**
     * Constructs an ATR indicator.
     *
     * @param forId  The identifier of the prerequisite signal to analyze.
     * @param config The structural configuration defining the smoothing period.
     */
    public ATR(String forId,
            IndicatorConfig config) {
        super("ATR." + forId, List.of(forId), config);
        this.period = config.getPeriod();
        this.adjacencyChecker = new AdjacencyChecker(forId, config.getGapTolerance());
        this.forId = forId;
    }

    @Override
    public CalculationResult onTick(Timeline.EventsAtTick tick) {

        if (!adjacencyChecker.isContingent(tick)) {
            return new CalculationResult.Failure("Data is not contiguous for id: " + forId);
        }

        if (previousEvent == null) {
            previousEvent = tick;
            return new CalculationResult.Waiting();
        }

        var tr1 = tick.forId(forId).getValue("high") - tick.forId(forId).getValue("low");
        var tr2 = Math.abs(tick.forId(forId).getValue("high") - previousEvent.forId(forId).getValue("close"));
        var tr3 = Math.abs(tick.forId(forId).getValue("low") - previousEvent.forId(forId).getValue("close"));

        var tr = Math.max(tr1, Math.max(tr2, tr3));

        if (Double.isNaN(tr)) {
            return new CalculationResult.Failure("TR value is NaN for id: " + forId);
        }

        if (trValues.size() < period) {
            trValues.offerLast(tr);
            return new CalculationResult.Waiting();
        } else {
            var previousAtr = trValues.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
            var atr = (previousAtr * (period - 1) + tr) / period;
            trValues.offerLast(tr);
            trValues.removeFirst();
            var iv = new IndicatorValue(tick.getTime(), atr, null);
            iv.setSignal(this);
            return new CalculationResult.Success(iv);
        }
    }
}
