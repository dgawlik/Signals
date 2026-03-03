package org.dgawlik.signals.indicator.counters;

public class TickCounting {

    private int count = 0;

    public void tick() {
        count++;
    }

    public double count() {
        return count;
    }

    public void reset() {
        count = 0;
    }
}
