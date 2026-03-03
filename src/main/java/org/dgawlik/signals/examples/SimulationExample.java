package org.dgawlik.signals.examples;

import org.dgawlik.signals.Frequency;
import org.dgawlik.signals.Simulation;
import org.dgawlik.signals.indicator.counters.TickCounting;
import org.dgawlik.signals.indicator.extensions.EMA;

public class SimulationExample {

    public static void main(String[] args) {
        var finalPortfolio = Simulation.forInstruments(Frequency.ONE_DAY, "AAPL", "TSLA")
                .withPortfolio(1_000_000, 2.5)
                .withIndicators(new EMA("AAPL", 7), new EMA("AAPL", 24), new EMA("TSLA", 7), new EMA("TSLA", 24))
                .onStart(context -> {
                    context.counters().put("AAPL", new TickCounting());
                    context.counters().put("TSLA", new TickCounting());
                })
                .run(context -> {
                    if (context.lookbehind().size() > 1) {
                        var prevQuote = context.lookbehind().get(context.lookbehind().size() - 2);
                        var quote = context.lookbehind().getLast();

                        var prevEma7Aapl = prevQuote.getIndicator("EMA7.AAPL").val1();
                        var ema7Aapl = quote.getIndicator("EMA7.AAPL").val1();

                        var prevEma24Aapl = prevQuote.getIndicator("EMA24.AAPL").val1();
                        var ema24Aapl = quote.getIndicator("EMA24.AAPL").val1();

                        var prevEma7Tsla = prevQuote.getIndicator("EMA7.TSLA").val1();
                        var ema7Tsla = quote.getIndicator("EMA7.TSLA").val1();

                        var prevEma24Tsla = prevQuote.getIndicator("EMA24.TSLA").val1();
                        var ema24Tsla = quote.getIndicator("EMA24.TSLA").val1();

                        try {
                            if (prevEma7Aapl < prevEma24Aapl && ema7Aapl > ema24Aapl) {
                                context.portfolio().ops(quote).buy("AAPL", 450_000).commit();
                                context.put("aaplOn", true);
                            }

                            if (prevEma7Tsla < prevEma24Tsla && ema7Tsla > ema24Tsla) {
                                context.portfolio().ops(quote).buy("TSLA", 450_000).commit();
                                context.put("tslaOn", true);
                            }

                            if (context.get("aaplOn") != null) {
                                context.getCounter("AAPL").add(0.0);

                                if (context.getCounter("AAPL").getValue() == 10) {
                                    context.portfolio().ops(quote).close("AAPL").commit();
                                    context.remove("aaplOn");
                                    context.getCounter("AAPL").reset();
                                }
                            }

                            if (context.get("tslaOn") != null) {
                                context.getCounter("TSLA").add(0.0);

                                if (context.getCounter("TSLA").getValue() == 10) {
                                    context.portfolio().ops(quote).close("TSLA").commit();
                                    context.remove("tslaOn");
                                    context.getCounter("TSLA").reset();
                                }
                            }

                        } catch (Exception e) {
                            System.err.println(e.getMessage());
                        }
                    }

                }).getResult();

        System.out.println(finalPortfolio.currentValuation().totalValue());
    }
}
