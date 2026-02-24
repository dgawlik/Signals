package org.dgawlik.signals.portfolio;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents a single holding of a particular asset in the {@link Portfolio}.
 * Exposes core tracking properties such as fractional unit composition and
 * dynamically tracked values.
 */
@Data
public class Position {
    /** The ticker or symbol identifier. */
    private String id;
    /** The amount or quantity of assets held. */
    private double units;
    /** The last polled closing price of the asset. */
    private double currentPrice;
    /** The baseline unit price upon which to derive PnL. */
    private double boughtAtPrice;
    /** The timestamp mapping the inception of the position. */
    private LocalDateTime boughtTime;
}
