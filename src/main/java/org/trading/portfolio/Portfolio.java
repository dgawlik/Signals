package org.trading.portfolio;

import com.jakewharton.fliptables.FlipTable;
import org.trading.Timeline;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acts as the centralized manager for holding positions, tracking cash
 * balances, and executing
 * simulated buy/sell orders against the historical data in the
 * {@link Timeline}.
 */
public class Portfolio {

    /**
     * A simple record to snapshot profit and loss statistics for an asset.
     */
    public record PnL(String id, double initialValue, double currentValue) {

        /**
         * @return The absolute monetary value of profit or loss.
         */
        public double pnl() {
            return currentValue - initialValue;
        }

        /**
         * @return The percentage return on investment.
         */
        public double pnlPercent() {
            return pnl() * 100 / initialValue;
        }
    }

    private List<Position> positions = new ArrayList<>();
    private Timeline timeline;
    private double cash;
    private double initialCash;
    private double transactionCosts;

    /**
     * Constructs a new Portfolio with an initial cash injection and a flat
     * transaction cost fee.
     *
     * @param timeline         The historical time series to query asset prices
     *                         during execution.
     * @param cash             The starting account balance in USD.
     * @param transactionCosts The flat fixed cost (in USD) incurred per trade.
     */
    public Portfolio(Timeline timeline, double cash, double transactionCosts) {
        this.timeline = timeline;
        this.initialCash = this.cash = cash;
        this.transactionCosts = transactionCosts;
    }

    /**
     * Executes a simulated purchase of an asset based on its closing price at a
     * specific time step.
     * Deducts the purchase amount and transaction costs from the cash pile.
     *
     * @param id              The asset symbol to buy.
     * @param amountInDollars The total dollar amount to allocate towards the asset.
     * @param time            The exact timestamp at which the trade should execute.
     */
    public void buy(String id, double amountInDollars, LocalDateTime time) {
        if (cash < amountInDollars + transactionCosts) {
            throw new IllegalArgumentException("Not enough cash");
        }
        cash -= amountInDollars + transactionCosts;

        var tick = timeline.getEvents(time, time, id);
        var price = tick.get(0).forId(id).getValue("close");
        var units = amountInDollars / price;

        var position = new Position();
        position.setId(id);
        position.setUnits(units);
        position.setBoughtAtPrice(price);
        position.setCurrentPrice(price);
        position.setBoughtTime(time);

        this.positions.add(position);
    }

    /**
     * Executes a simulated sale of an existing position based on its closing price
     * at a specific time step.
     * Unwinds the position and adds the retrieved cash value back to the portfolio
     * balance, minus fees.
     *
     * @param id              The asset symbol to sell.
     * @param amountInDollars The dollar value of the active position to liquidate.
     * @param time            The chronological time of execution. Must not precede
     *                        the initial purchase.
     */
    public void sell(String id, double amountInDollars, LocalDateTime time) {
        if (this.positions.stream().anyMatch(p -> p.getBoughtTime().isAfter(time))) {
            throw new IllegalArgumentException("Can't go back in time");
        }

        var position = this.positions.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();

        if (position.isEmpty()) {
            throw new IllegalArgumentException("No position found");
        }

        updateValuations(time);

        var valuation = position.get().getUnits() * position.get().getCurrentPrice();

        if (valuation < amountInDollars + transactionCosts) {
            throw new IllegalArgumentException("Not enough cash");
        }

        cash += amountInDollars - transactionCosts;

        position.get().setUnits(position.get().getUnits() - (amountInDollars / position.get().getCurrentPrice()));

        if (position.get().getUnits() == 0) {
            this.positions.remove(position.get());
        }
    }

    /**
     * Recalculates the current monetary valuations for all active held positions by
     * polling their most
     * recent close prices from the timeline.
     *
     * @param time The time cursor at which to gauge the valuation.
     */
    public void updateValuations(LocalDateTime time) {
        var ids = positions.stream().map(Position::getId).toList();

        var tick = timeline.getEvents(time, time, ids.toArray(new String[0]));
        for (var position : positions) {
            var price = tick.get(0).forId(position.getId()).getValue("close");
            position.setCurrentPrice(price);
        }
    }

    /**
     * Generates a single overarching profit/loss report encompassing the starting
     * cash vs the
     * dynamically updated current net liquidation value of the portfolio.
     *
     * @param time Evaluates performance up to this timestamp.
     * @return The aggregated portfolio-wide PnL metric.
     */
    public PnL totalPnL(LocalDateTime time) {
        updateValuations(time);

        var total = cash;
        for (var position : positions) {
            total += position.getUnits() * position.getCurrentPrice();
        }

        return new PnL("total", initialCash, total);
    }

    /**
     * Generates a breakdown of the profit and loss status for each independently
     * held position.
     *
     * @param time The valuation timestamp.
     * @return A list mapping specific assets dynamically to their current PnL
     *         performance.
     */
    public List<PnL> pnLs(LocalDateTime time) {
        updateValuations(time);

        return positions.stream()
                .map(p -> new PnL(p.getId(), p.getBoughtAtPrice() * p.getUnits(), p.getCurrentPrice() * p.getUnits()))
                .toList();
    }

    /**
     * Prints a visually formatted ASCII table detailing the current status and
     * performance metrics
     * of holding assets within the portfolio.
     *
     * @param stream The output stream destination (e.g., System.out).
     * @param time   The timestamp contextualizing the snapshot.
     */
    public void print(PrintStream stream, LocalDateTime time) {
        stream.println("==== { Portfolio } ==== ");
        stream.println("As of : " + time);

        String[] headers = { "Id", "PnL", "PnL %" };

        var pnls = pnLs(time);

        List<String[]> data = new ArrayList<>();
        for (var pnl : pnls) {
            data.add(new String[] {
                    pnl.id(),
                    "" + pnl.pnl(),
                    "" + pnl.pnlPercent()
            });
        }

        var data2 = data.toArray(new String[0][]);

        stream.println(FlipTable.of(headers, data2));

    }
}
