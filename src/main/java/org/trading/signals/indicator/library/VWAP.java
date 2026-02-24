package org.trading.signals.indicator.library;

import org.trading.Timeline;
import org.trading.signals.indicator.AdjacencyChecker;
import org.trading.signals.indicator.BaseIndicator;
import org.trading.signals.indicator.CalculationResult;
import org.trading.signals.indicator.IndicatorConfig;
import org.trading.signals.indicator.IndicatorValue;

import java.util.LinkedList;
import java.util.List;

/**
 * Volume Weighted Average Price (VWAP).
 * An indicator used to measure the average price a security traded at
 * throughout the day,
 * based on both volume and price. It is important because it provides a
 * benchmark for
 * price action and institutional purchasing.
 */
public class VWAP extends BaseIndicator {

    record PriceVolume(double price, double volume) {
    }

    private final int period;
    private final String forId;
    private Timeline.EventsAtTick previousEvent;
    private LinkedList<PriceVolume> priceVols = new LinkedList<>();
    private AdjacencyChecker adjacencyChecker;

    /**
     * Constructs a VWAP indicator.
     *
     * @param forId  The identifier of the prerequisite signal providing both price
     *               and volume data.
     * @param config The configuration specifying the internal calculation window.
     */
    public VWAP(String forId,
            IndicatorConfig config) {
        super("VWAP." + forId, List.of(forId), config);
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
        var volume = currentEvent.getValue("volume");

        if (Double.isNaN(price) || Double.isNaN(volume)) {
            return new CalculationResult.Waiting();
        }

        priceVols.offerLast(new PriceVolume(price, volume));

        if (priceVols.size() < period) {
            return new CalculationResult.Waiting();
        } else {
            var vwap = priceVols.stream().mapToDouble(pv -> pv.price * pv.volume).sum()
                    / priceVols.stream().mapToDouble(pv -> pv.volume).sum();
            priceVols.removeFirst();
            var iv = new IndicatorValue(tick.getTime(), vwap, null);
            iv.setSignal(this);
            return new CalculationResult.Success(iv);
        }
    }
}
