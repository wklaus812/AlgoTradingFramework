package bot.strategy;

import bot.testutils.TestSeries;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmaCrossStrategyFactoryTest {

    // Builds an array of close prices
    private static double[] closes(double base, double middle, double last) {
        double[] closePrices = new double[46];
        Arrays.fill(closePrices, 0, 40, base);       // 40 flat bars: EMA settles at base
        Arrays.fill(closePrices, 40, 45, middle);    // 5 bars on one side of the EMA
        closePrices[45] = last;                      // the bar that crosses
        return closePrices;
    }

    @Test
    void entersWhenPriceCrossesAboveEmaTest() {
        BarSeries series = TestSeries.fromCloses(0.002, closes(1.1200, 1.1000, 1.1400));
        Strategy strategy = new EmaCrossStrategyFactory().build(series);

        assertFalse(strategy.shouldEnter(44));   // still below the EMA
        assertTrue(strategy.shouldEnter(45));    // crossed up
        assertFalse(strategy.shouldExit(45));
    }

    @Test
    void exitsWhenPriceCrossesBelowEmaTest() {
        BarSeries series = TestSeries.fromCloses(0.002, closes(1.1200, 1.1400, 1.1000));
        Strategy strategy = new EmaCrossStrategyFactory().build(series);

        assertFalse(strategy.shouldExit(44));
        assertTrue(strategy.shouldExit(45));
        assertFalse(strategy.shouldEnter(45));
    }
}
