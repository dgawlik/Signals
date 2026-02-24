package org.trading.signals;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.trading.EventFrequency;
import org.trading.Timeline;
import org.trading.signals.indicator.IndicatorConfig;
import org.trading.signals.indicator.library.EMA;
import org.trading.signals.indicator.library.VWAP;
import org.trading.signals.ticker.Ticker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

public class EMATest {


    @Test
    public void test_positive() {
        var indicatorConfig = IndicatorConfig.builder()
                .gapTolerance(5)
                .period(7);

        var ema = new EMA("AAPL", indicatorConfig.build());

        var aapl = Ticker.defaultOf("AAPL");

        var timeline = Timeline.forBacktesting(EventFrequency.ONE_DAY);

        timeline.addSignal(aapl);
        timeline.addSignal(ema);

        var atrValues = timeline.getEvents(
                        LocalDateTime.of(
                                LocalDate.of(2026, 1, 1),
                                LocalTime.MIN),
                        LocalDateTime.of(
                                LocalDate.of(2026, 2, 22),
                                LocalTime.MIN
                        ), "AAPL", "EMA.AAPL"
                ).stream()
                .map(ev -> ev.forId("EMA.AAPL"))
                .filter(Objects::nonNull)
                .toList();

        Assertions.assertEquals(34, atrValues.size());
    }
}
