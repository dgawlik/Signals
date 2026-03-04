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
   var finalPortfolio = Simulation.forInstruments(Frequency.ONE_DAY, "AAPL", "TSLA")
        .withPortfolio(100_000, 2.5)
        .withIndicators(new EMA("AAPL", 7), new EMA("AAPL", 24), new EMA("TSLA", 7), new EMA("TSLA", 24))
        .run(() -> {

            var isAaplOn = new Boolean[] { false };
            var isTslaOn = new Boolean[] { false };

            var slAapl = new TrailingStopLoss();
            var slTsla = new TrailingStopLoss();

            return (portfolio, lookbehind) -> {
                if (lookbehind.size() > 1) {
                    var prevQuote = lookbehind.get(lookbehind.size() - 2);
                    var quote = lookbehind.getLast();

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
                            var numPositions = portfolio.currentValuation().positions().size();

                            if (numPositions == 0) {
                                portfolio.ops(quote).rebalance(Map.of("AAPL", 0.9)).commit();
                            } else {
                                portfolio.ops(quote)
                                        .rebalance(Map.of("AAPL", 0.48, "TSLA", 0.48))
                                        .commit();
                            }
                            isAaplOn[0] = true;
                            slAapl.setUp(0.05, quote.getCandle("AAPL").close());
                        }

                        if (prevEma7Tsla < prevEma24Tsla && ema7Tsla > ema24Tsla) {
                            var numPositions = portfolio.currentValuation().positions().size();

                            if (numPositions == 0) {
                                portfolio.ops(quote).rebalance(Map.of("TSLA", 0.9)).commit();
                            } else {
                                portfolio.ops(quote)
                                        .rebalance(Map.of("AAPL", 0.48, "TSLA", 0.48))
                                        .commit();
                            }

                            isTslaOn[0] = true;
                            slTsla.setUp(0.05, quote.getCandle("TSLA").close());
                        }

                        if (isAaplOn[0]) {
                            if (slAapl.isHit(quote.getCandle("AAPL").close())) {
                                portfolio.ops(quote).close("AAPL").commit();
                                isAaplOn[0] = false;
                            }
                        }

                        if (isTslaOn[0]) {
                            if (slTsla.isHit(quote.getCandle("TSLA").close())) {
                                portfolio.ops(quote).close("TSLA").commit();
                                isTslaOn[0] = false;
                            }
                        }

                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }

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

