package bot.strategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

public class EmaCrossStrategyFactory implements StrategyFactory {
    @Override
    public Strategy build(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator ema = new EMAIndicator(closePrice, 25);
        Rule buyingRule = new CrossedUpIndicatorRule(ema, closePrice);
        Rule sellingRule = new CrossedDownIndicatorRule(ema, closePrice);

        return new BaseStrategy(buyingRule, sellingRule);
    }
}
