package bot.strategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.StopGainRule;
import org.ta4j.core.rules.TrailingStopLossRule;

public class EmaCrossStrategyFactory implements StrategyFactory {
    @Override
    public Strategy build(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator ema = new EMAIndicator(closePrice, 25);
        Rule buyingRule = new CrossedUpIndicatorRule(ema, closePrice);
        Rule sellingRule = new CrossedDownIndicatorRule(ema, closePrice)
                .or(new TrailingStopLossRule(closePrice, series.numFactory().numOf(4)))     // Determine using average true range
                .or(new StopGainRule(closePrice, series.numFactory().numOf(3)));            // Determine using average true range

        return new BaseStrategy(buyingRule, sellingRule);
    }
}
