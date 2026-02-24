package org.trading.signals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.trading.EventFrequency;
import org.trading.Timeline;
import org.trading.signals.ticker.Ticker;

import java.time.LocalDateTime;

public class TickerTest {

    @Test
    public void test_on_attached() {

        var timeline = Timeline.forBacktesting(EventFrequency.ONE_DAY);

        var aapl = Ticker.defaultOf("AAPL");
        var pltr = Ticker.defaultOf("PLTR");
        var tsla = Ticker.defaultOf("TSLA");

        timeline.addSignal(aapl);
        timeline.addSignal(pltr);
        timeline.addSignal(tsla);

        var events = timeline.getEvents(
                LocalDateTime.of(2026, 2, 1, 0, 0, 0),
                LocalDateTime.of(2026, 2, 28, 0, 0, 0),
                "AAPL", "PLTR", "TSLA"
        );

        Assertions.assertEquals(14, events.size());
    }
}
