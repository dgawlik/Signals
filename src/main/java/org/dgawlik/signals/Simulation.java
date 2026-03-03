package org.dgawlik.signals;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.dgawlik.signals.etoro.CandlesEndpoint;
import org.dgawlik.signals.indicator.Series;
import org.dgawlik.signals.indicator.counters.Counter;
import org.dgawlik.signals.portfolio.Portfolio;

public class Simulation {

    public record Context(Portfolio portfolio, Map<String, Object> data, List<Quote> lookbehind,
            Map<String, Counter> counters) {

        public <T> T get(String key) {
            return (T) data.get(key);
        }

        public void put(String key, Object value) {
            data.put(key, value);
        }

        public void remove(String key) {
            data.remove(key);
        }

        public Counter getCounter(String key) {
            return counters.get(key);
        }

        public void putCounter(String key, Counter counter) {
            counters.put(key, counter);
        }

        public void removeCounter(String key) {
            counters.remove(key);
        }

    }

    private Portfolio portfolio;
    private int lookbehind = 10;
    private LocalDateTime from = LocalDateTime.MIN;
    private LocalDateTime to = LocalDateTime.MAX;

    private final List<SymbolEvents> events;
    private final Frequency frequency;

    private Context initialContext;

    private Simulation(Frequency frequency, String... intruments) {
        var candlesEndpoint = CandlesEndpoint.ETORO();
        this.events = candlesEndpoint.fetch(frequency, 1000, intruments);
        this.frequency = frequency;
    }

    public Simulation withPortfolio(double cashAmount, double feeAmount) {
        this.portfolio = Portfolio.create(cashAmount, feeAmount);
        return this;
    }

    public Simulation withIndicators(Series... indicators) {
        var aggregator = SeriesAggregator.create(0, frequency).addEvents(events);
        var quotes = aggregator.convertToQuotes();

        for (var indicator : indicators) {
            events.add(indicator.get(quotes));
        }
        return this;
    }

    public static Simulation forInstruments(Frequency frequency, String... intruments) {
        return new Simulation(frequency, intruments);
    }

    public Portfolio getResult() {
        return portfolio;
    }

    public Simulation withFrom(LocalDateTime from) {
        this.from = from;
        return this;
    }

    public Simulation withTo(LocalDateTime to) {
        this.to = to;
        return this;
    }

    public Simulation withLookbehind(int lookbehind) {
        this.lookbehind = lookbehind;
        return this;
    }

    public Simulation onStart(Consumer<Context> strategy) {
        this.initialContext = new Context(portfolio, new HashMap<>(), new LinkedList<>(), new HashMap<>());
        strategy.accept(initialContext);
        return this;
    }

    public Simulation run(Consumer<Context> strategy) {
        if (portfolio == null) {
            throw new IllegalStateException("Portfolio is not set");
        }

        var aggregator = SeriesAggregator.create(0, frequency).addEvents(events);
        var quotes = aggregator.convertToQuotes().stream()
                .filter(q -> !q.time(frequency).isBefore(from) && !q.time(frequency).isAfter(to)).toList();

        Map<String, Object> data;
        Map<String, Counter> counters;
        if (initialContext != null) {
            data = initialContext.data();
            counters = initialContext.counters();
        } else {
            data = new HashMap<String, Object>();
            counters = new HashMap<String, Counter>();
        }

        var lookbehindQueue = new LinkedList<Quote>();

        for (var quote : quotes) {
            if (lookbehindQueue.size() > lookbehind) {
                lookbehindQueue.removeFirst();
            }
            lookbehindQueue.addLast(quote);

            strategy.accept(new Context(portfolio, data, List.copyOf(lookbehindQueue), counters));
        }

        return this;
    }
}
