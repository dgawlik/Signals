package org.dgawlik.signals.signals;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.dgawlik.signals.EventFrequency;
import org.dgawlik.signals.Timeline;
import org.dgawlik.signals.signals.indicator.IndicatorConfig;
import org.dgawlik.signals.signals.indicator.library.RSI;
import org.dgawlik.signals.signals.ticker.Ticker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

public class RSITest {


    @Test
    public void test_positive() {
        var indicatorConfig = IndicatorConfig.builder()
                .gapTolerance(5)
                .period(14);

        var rsi = new RSI("AAPL", indicatorConfig.build());

        var aapl = Ticker.defaultOf("AAPL");

        var timeline = Timeline.forBacktesting(EventFrequency.ONE_DAY);

        timeline.addSignal(aapl);
        timeline.addSignal(rsi);

        var atrValues = timeline.getEvents(
                        LocalDateTime.of(
                                LocalDate.of(2026, 1, 1),
                                LocalTime.MIN),
                        LocalDateTime.of(
                                LocalDate.of(2026, 2, 22),
                                LocalTime.MIN
                        ), "AAPL", "RSI.AAPL"
                ).stream()
                .map(ev -> ev.forId("RSI.AAPL"))
                .filter(Objects::nonNull)
                .toList();

        Assertions.assertEquals(34, atrValues.size());
    }
}
