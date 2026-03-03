package org.dgawlik.signals.indicator.counters;

import java.util.LinkedList;

public class KellyRatio {

    private double timeHorizon;
    private LinkedList<Double> returns;

    public KellyRatio(double timeHorizon) {
        this.timeHorizon = timeHorizon;
        this.returns = new LinkedList<>();
    }

    public void addReturn(double r) {
        returns.add(r);

        if (returns.size() > timeHorizon) {
            returns.removeFirst();
        }
    }

    public double getKellyRatio() {
        if (returns.size() < timeHorizon) {
            return Double.NaN;
        } else {
            double b = returns.stream().mapToDouble(r -> r).average().orElse(Double.NaN);
            double p = returns.stream().filter(r -> r > 0).count() / (double) returns.size();
            double q = 1 - p;
            return (p * b - q) / b;
        }
    }

    public void reset() {
        returns.clear();
    }

    public boolean isReady() {
        return returns.size() >= timeHorizon;
    }

}
