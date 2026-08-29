package bot.strategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

public interface StrategyFactory {
    Strategy build(BarSeries series);
}
