package org.dgawlik.signals.signals.indicator.library;

import org.dgawlik.signals.Timeline;
import org.dgawlik.signals.signals.indicator.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Relative Strength Index (RSI).
 * A momentum oscillator that measures the speed and change of price movements.
 * RSI oscillates between 0 and 100. Traditionally, an asset is considered
 * overbought when
 * the RSI is above 70 and oversold when it is below 30.
 */
public class RSI extends BaseIndicator {

    record GainLoss(double gain, double loss) {
    }

    private final int period;
    private final String forId;
    private Timeline.EventsAtTick previuosTick;
    private LinkedList<GainLoss> gainlosses = new LinkedList<>();
    private AdjacencyChecker adjacencyChecker;

    /**
     * Constructs an RSI indicator.
     *
     * @param forId  The identifier of the prerequisite signal.
     * @param config The structural configuration defining the lookback period to
     *               compute average gains and losses.
     */
    public RSI(String forId,
            IndicatorConfig config) {
        super("RSI." + forId, List.of(forId), config);
        this.period = config.getPeriod();
        this.adjacencyChecker = new AdjacencyChecker(forId, config.getGapTolerance());
        this.forId = forId;
    }

    @Override
    public CalculationResult onTick(Timeline.EventsAtTick tick) {
        if (!adjacencyChecker.isContingent(tick)) {
            return new CalculationResult.Failure("Data is not contiguous for id: " + forId);
        }

        if (previuosTick == null) {
            previuosTick = tick;
            return new CalculationResult.Waiting();
        }

        var previousEvent = previuosTick.forId(forId);
        var currentEvent = tick.forId(forId);

        if (currentEvent == null) {
            return new CalculationResult.Waiting();
        }

        var priceChange = currentEvent.getValue("close") - previousEvent.getValue("close");

        if (Double.isNaN(priceChange)) {
            return new CalculationResult.Waiting();
        }

        var gain = Math.max(priceChange, 0);
        var loss = Math.min(priceChange, 0);

        gainlosses.offerLast(new GainLoss(gain, loss));

        if (gainlosses.size() < period) {
            return new CalculationResult.Waiting();
        } else {
            var avgGain = gainlosses.stream().mapToDouble(gl -> gl.gain).average().orElse(Double.NaN);
            var avgLoss = gainlosses.stream().mapToDouble(gl -> gl.loss).average().orElse(Double.NaN);

            gainlosses.removeFirst();

            if (avgLoss == 0) {
                return new CalculationResult.Success(new IndicatorValue(tick.getTime(), 100.0, null));
            }

            var rs = avgGain / Math.abs(avgLoss);
            var rsi = 100 - (100 / (1 + rs));

            previuosTick = tick;

            var iv = new IndicatorValue(tick.getTime(), rsi, null);
            iv.setSignal(this);
            return new CalculationResult.Success(iv);
        }
    }
}
