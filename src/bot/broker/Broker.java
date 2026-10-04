package bot.broker;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import bot.engine.OrderDetails;
import java.math.BigDecimal;

public interface Broker {
    BigDecimal getAccountBalance();
    BarSeries getHistoricalBarSeries(String instrument);
    Bar getLatestBar(String instrument);
    String placeMarketOrder(String instrument, int tradeSize, OrderDetails details);
    boolean closePosition(String instrument, String tradeId);
    BigDecimal getClosePriceInUsd(String insstrument);
    String getBrokerName();
}
