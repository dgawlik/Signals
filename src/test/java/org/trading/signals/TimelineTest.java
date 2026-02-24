package org.trading.signals;


import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.trading.EventFrequency;
import org.trading.Timeline;
import org.trading.signals.ticker.Candle;
import org.trading.signals.ticker.Ticker;
import org.trading.signals.ticker.TickerConfig;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TimelineTest {

    @Test
    public void test_single_event_add() {
        var timeline = Timeline.forBacktesting(EventFrequency.ONE_DAY);

        var tc = TickerConfig.builder()
                .id("AAPL")
                .build();

        var ticker = new Ticker(tc);
        var candle = new Candle(
                LocalDateTime.of(2026, 2, 21, 21, 49, 5),
                0.0, 0.0, 0.0, 0.0, Optional.of(0.0)
        );
        candle.setSignal(ticker);

        timeline.addEvents(List.of(candle));

        var events = timeline.getEvents(
                LocalDateTime.of(2026, 2, 20, 0, 0, 0),
                LocalDateTime.of(2026, 2, 22, 0, 0, 0),
                "AAPL"
        );

        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(LocalDateTime.of(2026, 2, 21, 0, 0, 0), events.get(0).getTime());


    }

    @Test
    public void test_multiple_events_best_effort_last_wins() {
        var timeline = Timeline.forBacktesting(EventFrequency.ONE_DAY);

        var tc = TickerConfig.builder()
                .id("AAPL")
                .build();

        var ticker = new Ticker(tc);
        var candle = new Candle(
                LocalDateTime.of(2026, 2, 21, 21, 49, 5),
                0.0, 0.0, 0.0, 0.0, Optional.of(0.0)
        );
        var candle2 = new Candle(
                LocalDateTime.of(2026, 2, 21, 21, 49, 6),
                1.0, 1.0, 1.0, 1.0, Optional.of(1.0)
        );
        candle.setSignal(ticker);
        candle2.setSignal(ticker);


        timeline.addEvents(List.of(candle, candle2));

        var events = timeline.getEvents(
                LocalDateTime.of(2026, 2, 20, 0, 0, 0),
                LocalDateTime.of(2026, 2, 22, 0, 0, 0),
                "AAPL"
        );

        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(LocalDateTime.of(2026, 2, 21, 0, 0, 0), events.get(0).getTime());

        Assertions.assertEquals(1.0, events.get(0).forId("AAPL").getValue("close"));
    }

    @Test
    public void test_multiple_events_best_effort_last_wins_2() {
        var timeline = Timeline.forBacktesting(EventFrequency.ONE_DAY);

        var tc = TickerConfig.builder()
                .id("AAPL")
                .build();

        var ticker = new Ticker(tc);
        var candle = new Candle(
                LocalDateTime.of(2026, 2, 21, 21, 49, 5),
                0.0, 0.0, 0.0, 0.0, Optional.of(0.0)
        );
        var candle2 = new Candle(
                LocalDateTime.of(2026, 2, 21, 21, 49, 6),
                1.0, 1.0, 1.0, 1.0, Optional.of(1.0)
        );
        candle.setSignal(ticker);
        candle2.setSignal(ticker);


        timeline.addEvents(List.of(candle));
        timeline.addEvents(List.of(candle2));


        var events = timeline.getEvents(
                LocalDateTime.of(2026, 2, 20, 0, 0, 0),
                LocalDateTime.of(2026, 2, 22, 0, 0, 0),
                "AAPL"
        );

        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(LocalDateTime.of(2026, 2, 21, 0, 0, 0), events.get(0).getTime());

        Assertions.assertEquals(1.0, events.get(0).forId("AAPL").getValue("close"));
    }

    @Test
    public void test_multiple_events_different_ids() {
        var timeline = Timeline.forBacktesting(EventFrequency.ONE_DAY);

        var tc = TickerConfig.builder()
                .id("AAPL")
                .build();
        var tc2 = TickerConfig.builder()
                .id("MSFT")
                .build();


        var ticker = new Ticker(tc);
        var ticker2 = new Ticker(tc2);

        var candle = new Candle(
                LocalDateTime.of(2026, 2, 21, 21, 49, 5),
                0.0, 0.0, 0.0, 0.0, Optional.of(0.0)
        );
        var candle2 = new Candle(
                LocalDateTime.of(2026, 2, 21, 21, 49, 6),
                1.0, 1.0, 1.0, 1.0, Optional.of(1.0)
        );
        candle.setSignal(ticker);
        candle2.setSignal(ticker2);


        timeline.addEvents(List.of(candle));
        timeline.addEvents(List.of(candle2));

        var events = timeline.getEvents(
                LocalDateTime.of(2026, 2, 20, 0, 0, 0),
                LocalDateTime.of(2026, 2, 22, 0, 0, 0),
                "AAPL", "MSFT"
        );

        Assertions.assertEquals(1, events.size());
        Assertions.assertNotNull(events.get(0).forId("AAPL"));
        Assertions.assertNotNull(events.get(0).forId("MSFT"));

        Assertions.assertNull(events.get(0).forId("META"));
    }

    @Test
    @SneakyThrows
    public void test_live_timeline() {
        var timeline = Timeline.forLive();

        var tickers = Ticker.defaultTickers("PLTR", "AAPL");

        timeline.addSignals(tickers);

        var events = new ArrayList<Timeline.EventsAtTick>();

        timeline.notify(events::add);

        Thread.sleep(25_000);

        Assertions.assertFalse(events.isEmpty());
    }
}
