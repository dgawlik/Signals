package org.trading.signals.indicator;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.trading.signals.Signal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An event implementation created uniquely by an Indicator denoting the outcome
 * of a numerical calculation
 * at a specific point in time. It typically yields primary indicator
 * measurements (and optional secondary bands).
 */
@RequiredArgsConstructor
@Data
public class IndicatorValue implements Signal.Event {
    private final @NonNull LocalDateTime time;
    @NonNull
    private final Double primary;
    private final Double secondary;

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
        return List.of("primary", "secondary");
    }

    @Override
    public double getValue(String key) {
        return switch (key) {
            case "primary" -> {
                yield primary;
            }
            case "secondary" -> {
                if (secondary == null) {
                    yield Double.NaN;
                }
                yield secondary;
            }
            default -> throw new IllegalArgumentException("Key not found");
        };
    }
}
