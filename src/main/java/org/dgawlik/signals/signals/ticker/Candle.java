package org.dgawlik.signals.signals.ticker;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dgawlik.signals.signals.Signal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a single period of price movement (a candlestick) emitted by a
 * {@link Ticker}.
 * It stores standard OHLCV (Open, High, Low, Close, Volume) data metrics.
 */
@Data
@RequiredArgsConstructor
public class Candle implements Signal.Event {
    private final @NonNull LocalDateTime time;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final @NonNull Optional<Double> volume;
    private Signal signal;

    @Override
    public Map<String, Object> getMetadata() {
        return this.signal.getMetadata();
    }

    @Override
    public String getId() {
        return this.signal.getId();
    }

    @Override
    public List<String> getKeys() {
        return List.of("open", "high", "low", "close", "volume");
    }

    @Override
    public double getValue(String key) {
        if (!getKeys().contains(key)) {
            throw new IllegalArgumentException("Key not found");
        }

        if (key.equals("volume") && volume.isEmpty()) {
            return Double.NaN;
        }

        return switch (key) {
            case "open" -> open;
            case "high" -> high;
            case "low" -> low;
            case "close" -> close;
            case "volume" -> volume.get();
            default -> throw new IllegalStateException("Unexpected value: " + key);
        };
    }
}
