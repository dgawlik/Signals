package org.trading.portfolio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.trading.EventFrequency;
import org.trading.Timeline;
import org.trading.signals.ticker.Ticker;

import java.time.LocalDateTime;
import java.util.List;

public class PortfolioTest {


    @Test
    public void test_positive_case() {

        var timeline = Timeline.forBacktesting(EventFrequency.ONE_DAY);

        var tickers = Ticker.defaultTickers("SMH", "PLTR", "FALN", "JEPQ", "COPX", "IBIT");

        timeline.addSignals(tickers);

        var portfolio = new Portfolio(timeline, 1_000_000.0, 0.0);

        var oneYearAgo = LocalDateTime.of(2025, 1, 2, 0, 0, 0);

        portfolio.buy("SMH", 200_000.0, oneYearAgo);
        portfolio.buy("PLTR", 200_000.0, oneYearAgo);
        portfolio.buy("FALN", 100_000.0, oneYearAgo);
        portfolio.buy("JEPQ", 100_000.0, oneYearAgo);
        portfolio.buy("COPX", 150_000.0, oneYearAgo);
        portfolio.buy("IBIT", 150_000.0, oneYearAgo);

        var now = LocalDateTime.of(2026, 1, 2, 0, 0, 0);

        Assertions.assertEquals(47.930140116577036, portfolio.totalPnL(now).pnlPercent());

        portfolio.print(System.out, now);
    }
}
