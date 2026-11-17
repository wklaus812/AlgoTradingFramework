package bot.testutils;

import bot.strategy.StrategyFactory;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

public class TestStrategyFactory implements StrategyFactory  {
    Strategy strategy = null;
    @Override
    public Strategy build(BarSeries series) {
        return strategy == null ? new TestStrategy() : strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }
}
