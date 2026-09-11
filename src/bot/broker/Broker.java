package bot.broker;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import bot.engine.OrderDetails;
import java.math.BigDecimal;

public interface Broker<T, S> {
    BigDecimal getAccountBalance();
    BarSeries getHistoricalBarSeries(S instrument);
    Bar getLatestBar(S instrument);
    T placeMarketOrder(S instrument, int tradeSize, OrderDetails details);
    void closePosition(S instrument, T tradeId);
    BigDecimal getClosePriceInUsd(S insstrument);
    String getBrokerName();
}
