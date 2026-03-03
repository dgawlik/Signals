package org.dgawlik.signals.indicator.counters;

import java.util.LinkedList;

public class VaR {

    private double portfolioValue;
    private double confidenceLevel = 1.65; // for 95% confidence level
    private double timeHorizon;
    private LinkedList<Double> returns;

    public VaR(double portfolioValue, double timeHorizon) {
        this.portfolioValue = portfolioValue;
        this.timeHorizon = timeHorizon;
        this.returns = new LinkedList<>();
    }

    public void addReturn(double r) {
        returns.add(r);

        if (returns.size() > timeHorizon) {
            returns.removeFirst();
        }
    }

    public double getVaR() {
        if (returns.size() < timeHorizon) {
            return Double.NaN;
        } else {
            var mu = returns.stream().mapToDouble(r -> r).average().orElse(Double.NaN);
            var sigma = Math.sqrt(returns.stream().mapToDouble(r -> (r - mu) * (r - mu)).average().orElse(Double.NaN));
            return portfolioValue * (mu - confidenceLevel * sigma);
        }
    }

    public void reset() {
        returns.clear();
    }

    public boolean isReady() {
        return returns.size() >= timeHorizon;
    }
}
