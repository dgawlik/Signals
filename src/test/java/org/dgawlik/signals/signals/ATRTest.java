package org.dgawlik.signals.signals;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.dgawlik.signals.EventFrequency;
import org.dgawlik.signals.Timeline;
import org.dgawlik.signals.signals.indicator.IndicatorConfig;
import org.dgawlik.signals.signals.indicator.library.ATR;
import org.dgawlik.signals.signals.ticker.Ticker;

import java.time.LocalDateTime;
import java.util.Objects;

public class ATRTest {


    @Test
    public void test_positive() {
        var indicatorConfig = IndicatorConfig.builder()
                .gapTolerance(5)
                .period(14);

        var atr = new ATR("AAPL", indicatorConfig.build());

        var aapl = Ticker.defaultOf("AAPL");

        var timeline = Timeline.forBacktesting(EventFrequency.ONE_DAY);

        timeline.addSignal(aapl);
        timeline.addSignal(atr);

        var atrValues = timeline.getEvents(
                        LocalDateTime.MIN, LocalDateTime.MAX, "AAPL", "ATR.AAPL"
                ).stream()
                .map(ev -> ev.forId("ATR.AAPL"))
                .filter(Objects::nonNull)
                .toList();

        Assertions.assertEquals(985, atrValues.size());
    }
}
