# Signals v2.0.0

## About

Project's inspiration was the early release of EToro trading API. Previous version had it's strong sides but
it had also pain points that are completely fixed now. Currently library is an:
* API client for EToro
* backtesting library

In future versions support of websocket monitoring of assets is planned and a decent set of indicators.

## Setup

Add this to your pom:

```xml
<repositories>
  <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
  </repository>
</repositories>
```

```xml
<dependency>
    <groupId>com.github.dgawlik</groupId>
    <artifactId>signals</artifactId>
    <version>cd292b73dc</version>
</dependency>
```

Then create in the root project .env file:

```
ETORO_PUBLIC_KEY="<your etoro public key>"
ETORO_API_KEY="<your etoro api key>"
```

## First taste

Here is what MACD following would look like for 7-period and 24-period EMAs.

```java
var finalPortfolio = Simulation.forInstruments(Frequency.ONE_DAY, "AAPL", "TSLA", "GOOG", "MSFT")
        .withPortfolio(100_000, 2.5)
        .withIndicators(
                new EMA("AAPL", 7),
                new EMA("AAPL", 24),
                new EMA("TSLA", 7),
                new EMA("TSLA", 24),
                new EMA("GOOG", 7),
                new EMA("GOOG", 24),
                new EMA("MSFT", 7),
                new EMA("MSFT", 24))
        .run(() -> {

            class MACDAndStopLossStrategy extends Strategy {

                private final String symbol;
                private final List<String> allSymbols;
                private TrailingStopLoss sl;

                public MACDAndStopLossStrategy(String symbol, List<String> allSymbols) {
                    this.symbol = symbol;
                    this.allSymbols = allSymbols;
                }

                @Override
                public void activateOn(List<Quote> lookbehind) {

                    if (lookbehind.size() > 1) {
                        var prevQuote = lookbehind.get(lookbehind.size() - 2);
                        var quote = lookbehind.getLast();

                        var prevEma7 = prevQuote.getIndicator("EMA7." + symbol).val1();
                        var ema7 = quote.getIndicator("EMA7." + symbol).val1();

                        var prevEma24 = prevQuote.getIndicator("EMA24." + symbol).val1();
                        var ema24 = quote.getIndicator("EMA24." + symbol).val1();

                        if (prevEma7 < prevEma24 && ema7 > ema24) {
                            active = true;
                            sl = new TrailingStopLoss();
                            sl.setUp(0.05, quote.getCandle(symbol).close());
                        }
                    }
                }

                @Override
                public void deactivateOn(Portfolio portfolio, List<Quote> lookbehind) {
                    var quote = lookbehind.getLast();

                    if (sl.isHit(quote.getCandle(symbol).close())) {
                        portfolio.ops(quote).close(symbol).commit();
                        active = false;
                    }
                }

                @Override
                public void act(Portfolio portfolio, List<Quote> lookbehind) {
                    var numPositions = portfolio.currentValuation().positions().size();
                    var quote = lookbehind.getLast();

                    if (numPositions == 0) {
                        portfolio.ops(quote).rebalance(Map.of(symbol, 0.9)).commit();
                    } else {
                        portfolio.ops(quote)
                                .rebalanceEqualWeights(0.95, allSymbols)
                                .commit();
                    }
                }
            }

            var strategyAapl = new MACDAndStopLossStrategy("AAPL", List.of("AAPL", "TSLA", "GOOG", "MSFT"));
            var strategyTsla = new MACDAndStopLossStrategy("TSLA", List.of("AAPL", "TSLA", "GOOG", "MSFT"));
            var strategyGoog = new MACDAndStopLossStrategy("GOOG", List.of("AAPL", "TSLA", "GOOG", "MSFT"));
            var strategyMsft = new MACDAndStopLossStrategy("MSFT", List.of("AAPL", "TSLA", "GOOG", "MSFT"));

            return (portfolio, lookbehind) -> {
                strategyAapl.step(portfolio, lookbehind);
                strategyTsla.step(portfolio, lookbehind);
                strategyGoog.step(portfolio, lookbehind);
                strategyMsft.step(portfolio, lookbehind);
            };
        }).getResult();

System.out.println(finalPortfolio.currentValuation().totalValue());
```

You create simulation for particular instruments as here *AAPL* and *TSLA*. Any ticker visible on EToro works.
Then for those primary signals you can add derivative signals (which are EMAs for now). 

A strategy is a function acting on portfolio which you get from context. You also have in context the lookbehind quotes
(a number that is configurable) and Map for storing any custom data between iterations.

Portfolio has ops() method that edits it in place, but each change is recorded. Remember to call commit after doing ops on it. It
is very restrictive regarding the operations that cannot be done. Mostly it throws IllegalStateException so you can fallback to
try catch blocks for quick iterations. 

The result is portfolio evaluated. As mentioned each change records current valuation, you can also call valuation manually. Then
you can graph how valuations changed between times.

## Concepts

**CandlesEndpoint** is the backbone of the project. It has a singleton method

```java
public static CandlesEndpoint ETORO() {
    Dotenv e = Dotenv.load();

    var publicKey = e.get("ETORO_PUBLIC_KEY");
    var apiKey = e.get("ETORO_API_KEY");

    return new CandlesEndpoint("https://public-api.etoro.com", publicKey, apiKey);
}
```

when you want to fetch candles you invoke

```
candlesEndpoint.fetch(Frequency.ONE_DAY, 100, "AAPL", "TSLA")
```

for example which is one of many frequencies, number of candles and fetched tickers.

**Event** is anything that happens and can be plotted. It is GADT of three types: 
* *Candle* with open, close, high, low and volume
* *indicator* that has val1 through val4 for data (double)
* *custom* anything that has not been thought of

**SeriesAggregator** it takes series of events, checks there are no more than configured gaps and produces **Quotes** that are 
crosssection across all tickers at the momemnt.

For example:

```java
var candlesEndpoint = CandlesEndpoint.ETORO();

var candles = candlesEndpoint.fetch(Frequency.ONE_DAY, 100, "AAPL", "GOOG", "SPX");
var aggregator = SeriesAggregator.create(0, Frequency.ONE_DAY).addEvents(candles);
```

**Series** is an indicator concept and a base clase for indicators not implemented yet. It provides the common algorithm and lookback
and all you do is to implement 

```java
 public abstract Event process(List<Quote> lookbehind) throws RuntimeException;
```

It you can throw exception inside and the interpretation will vary:
* `LogContinueException` continue executing logging the error only
* `LogDrainContinueException` log error and drain the lookback
* `BreakException` fatal exception that interrupts the calculations

**Portfolio** tracks the basket of goods. It can have one or many valuations. Valuation is the ticker, units and price set in stone. WHen 
you perform `ops()` on portfolio and then commit the changes you automatically create new valuation. You can also valuate the portfolio at
any given moment manually.


## Examples

Take a look at the examples in `src\main\java\org\dgawlik\signals\examples` directory.

## LICENSE

MIT License

