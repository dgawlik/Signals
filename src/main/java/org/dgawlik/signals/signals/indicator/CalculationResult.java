package org.dgawlik.signals.signals.indicator;

/**
 * Represents the bounded outcome space of a single tick calculation by an
 * Indicator.
 */
public sealed interface CalculationResult {

    /**
     * Signifies that the mathematical calculation worked properly and produced a
     * final value.
     */
    record Success(IndicatorValue value) implements CalculationResult {
    }

    /**
     * Indicates that processing halted exceptionally due to corrupt, missing, or
     * discontinuous data.
     */
    record Failure(String errorMessage) implements CalculationResult {
    }

    /**
     * Indicates that the indicator has safely registered the tick's data but has
     * not breached its
     * warmup period necessary to compute a meaningful value yet.
     */
    record Waiting() implements CalculationResult {
    }
}
