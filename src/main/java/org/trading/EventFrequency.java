package org.trading;

/**
 * Defines the frequencies at which events or candles are aggregated and
 * emitted.
 * This determines the discrete time steps used by the Timeline during
 * backtesting or live processing.
 */
public enum EventFrequency {

    TICK("Second"), ONE_MINUTE("OneMinute"),
    FIVE_MINUTES("FiveMinutes"), TEN_MINUTES("TenMinutes"),
    FIFTEEN_MINUTES("FifteenMinutes"), THIRTY_MINUTES("ThirtyMinutes"),
    ONE_HOUR("OneHour"), FOUR_HOURS("FourHours"), ONE_DAY("OneDay");

    private final String name;

    EventFrequency(String name) {
        this.name = name;
    }

    /**
     * Gets the string representation of the frequency format.
     * Often used to map logical frequencies to external API parameters (e.g. eToro
     * candle intervals).
     *
     * @return the name string for the frequency.
     */
    public String getName() {
        return this.name;
    }
}
