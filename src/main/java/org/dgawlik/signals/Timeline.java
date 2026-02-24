package org.dgawlik.signals;

import lombok.Getter;
import lombok.NonNull;
import org.dgawlik.signals.signals.Signal;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Represents the simulation or live timeline of the trading system.
 * It tracks and manages events sequentially in discrete time steps based on a
 * specific {@link EventFrequency}.
 * In backtesting, it acts as the primary time-series database. In live trading,
 * it emits real-time events.
 */
public class Timeline {

    /**
     * The frequency at which events are processed and grouped.
     */
    @Getter
    private final EventFrequency frequency;

    /**
     * A helper class that discretizes continuous time into intervals specified by
     * the Timeline's {@link EventFrequency}.
     * It allows grouping events that occur within the same time bucket.
     */
    private class DiscreteDateTime {

        private final LocalDateTime time;

        public DiscreteDateTime(LocalDateTime time) {
            this.time = time;
        }

        public LocalDateTime getTime() {
            var y = time.getYear();
            var m = time.getMonthValue();
            var d = time.getDayOfMonth();
            var h = time.getHour();
            var mi = time.getMinute();
            var s = time.getSecond();
            var frequency = Timeline.this.frequency;

            return switch (frequency) {
                case TICK -> LocalDateTime.of(y, m, d, h, mi, s);
                case ONE_MINUTE -> LocalDateTime.of(y, m, d, h, mi, 0);
                case FIVE_MINUTES -> LocalDateTime.of(y, m, d, h, (mi / 5) * 5, 0);
                case TEN_MINUTES -> LocalDateTime.of(y, m, d, h, (mi / 10) * 10, 0);
                case FIFTEEN_MINUTES -> LocalDateTime.of(y, m, d, h, (mi / 15) * 15, 0);
                case THIRTY_MINUTES -> LocalDateTime.of(y, m, d, h, (mi / 30) * 30, 0);
                case ONE_HOUR -> LocalDateTime.of(y, m, d, h, 0, 0);
                case FOUR_HOURS -> LocalDateTime.of(y, m, d, (h / 4) * 4, 0, 0);
                case ONE_DAY -> LocalDateTime.of(y, m, d, 0, 0, 0);
            };
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }

            if (other == this) {
                return true;
            }

            if (!(other instanceof DiscreteDateTime)) {
                return false;
            }

            return ((DiscreteDateTime) other).getTime().equals(this.getTime());
        }

        @Override
        public int hashCode() {
            return this.getTime().hashCode();
        }
    }

    /**
     * Container that groups all {@link Signal.Event}s that happen simultaneously at
     * a specific discrete time tick.
     */
    public class EventsAtTick {

        private final Map<String, Signal.Event> events;

        public EventsAtTick(Map<String, Signal.Event> events) {
            this.events = events;
        }

        /**
         * @return The frequency at which these grouped events occur.
         */
        public EventFrequency getFrequency() {
            return Timeline.this.frequency;
        }

        /**
         * Gives the discrete time bucket for the grouped events.
         *
         * @return the normalized local date time of the tick.
         */
        public LocalDateTime getTime() {
            var l = events.values().stream().toList();

            if (l.isEmpty()) {
                throw new IllegalStateException("No events");
            } else {
                return new DiscreteDateTime(l.getFirst().getTime()).getTime();
            }
        }

        /**
         * Retrieves the event associated with a specific signal or ticker ID at this
         * tick.
         *
         * @param id The ID of the signal or ticker.
         * @return the associated Event, or null if none occurred.
         */
        public Signal.Event forId(String id) {
            return events.get(id);
        }

        boolean setForId(String id, Signal.Event event) {
            var hasAlready = this.events.containsKey(id);
            this.events.put(id, event);
            return hasAlready;
        }

        EventsAtTick copy(Set<String> ids) {
            var intersection = new HashSet<>(this.events.keySet());
            intersection.retainAll(ids);

            if (intersection.isEmpty()) {
                return null;
            } else {
                var newMap = new HashMap<String, Signal.Event>();
                for (var id : intersection) {
                    newMap.put(id, this.events.get(id));
                }

                return new EventsAtTick(newMap);
            }
        }

        void merge(EventsAtTick other) {
            this.events.putAll(other.events);
        }

    }

    private final TreeSet<EventsAtTick> core;
    private final boolean isLive;
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static int MAX_EVENTS = 100_000;

    /**
     * Creates a Timeline instance configured for backtesting.
     *
     * @param frequency The discrete frequency at which simulation steps occur.
     * @return a new Timeline object in backtesting mode.
     */
    public static Timeline forBacktesting(EventFrequency frequency) {
        return new Timeline(frequency, false);
    }

    /**
     * Creates a Timeline instance configured for live trading mode over tick data.
     *
     * @return a new Timeline object in live mode.
     */
    public static Timeline forLive() {
        return new Timeline(EventFrequency.TICK, true);
    }

    private Timeline(EventFrequency frequency, boolean isLive) {
        this.frequency = frequency;
        this.core = new TreeSet<>(Comparator.comparing(EventsAtTick::getTime));
        this.isLive = isLive;
    }

    /**
     * Adds a collection of {@link Signal}s to the timeline, triggering their
     * backtest or live subscription setup.
     *
     * @param signals A list of signals to be managed by this timeline.
     */
    public void addSignals(List<Signal> signals) {
        signals.forEach(this::addSignal);
    }

    /**
     * Subscribes to periodic notifications of the most recent events in the
     * timeline.
     * This establishes an active loop operating every second to stream the latest
     * tick to the subscriber.
     *
     * @param externalSubscriber the consumer accepting tick events.
     */
    public void notify(Consumer<EventsAtTick> externalSubscriber) {
        scheduler.scheduleAtFixedRate(() -> {
            Thread.startVirtualThread(() -> {
                try {
                    var last = this.core.last();
                    if (last != null) {
                        externalSubscriber.accept(last);
                    }
                } catch (Exception e) {
                }
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Adds an individual {@link Signal} to the timeline, invoking either
     * backtesting data fetching
     * or establishing live updates based on the mode of the Timeline.
     *
     * @param signal The signal to attach.
     */
    public void addSignal(Signal signal) {
        if (!isLive) {
            signal.onBacktesting(this);
        } else {
            signal.onLiveUpdate(this::addEvents);
        }
    }

    /**
     * Retrieves an immutable list of ticking events that occurred within a given
     * timeframe, targeting specific signal IDs.
     *
     * @param from The inclusive start time for retrieval.
     * @param to   The inclusive end time for retrieval.
     * @param ids  The subset of IDs representing specific signals or tickers of
     *             interest.
     * @return The filtered list of events occurring from {@code from} to
     *         {@code to}.
     */
    public List<EventsAtTick> getEvents(LocalDateTime from, LocalDateTime to, String... ids) {
        if (ids.length == 0) {
            throw new IllegalArgumentException("No ids provided");
        }

        return this.core.stream()
                .filter(e -> !e.getTime().isBefore(from) && !e.getTime().isAfter(to))
                .map(ev -> ev.copy(Set.of(ids)))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Adds a batch of events to the core Timeline internal time-series,
     * discretizing their times and merging
     * them properly onto discrete time steps.
     * Avoids storing over {@code MAX_EVENTS}.
     *
     * @param events The raw list of Events to be appended or merged sequentially
     *               into the Timeline.
     */
    public void addEvents(@NonNull List<Signal.Event> events) {
        var grouped = events.stream()
                .collect(Collectors
                        .groupingBy(ev -> new DiscreteDateTime(ev.getTime()).getTime()));

        for (var partition : grouped.values()) {
            var tick = new EventsAtTick(new HashMap<>());
            for (var event : partition) {
                tick.setForId(event.getSignal().getId(), event);
            }

            var closest = this.core.floor(tick);

            if (closest != null && closest.getTime().equals(tick.getTime())) {
                closest.merge(tick);
            } else {
                this.core.add(tick);

                if (this.core.size() > MAX_EVENTS) {
                    this.core.pollFirst();
                }
            }
        }
    }
}
