package main.broker;

import main.engine.FilledTradeDetails;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import main.engine.OrderDetails;
import java.math.BigDecimal;

public interface Broker {
    BigDecimal getAccountBalance();
    BarSeries getHistoricalBarSeries(String instrument);
    Bar getLatestBar(String instrument);
    FilledTradeDetails placeMarketOrder(String instrument, int tradeSize, OrderDetails details);
    FilledTradeDetails closePosition(String instrument, String tradeId);
    BigDecimal getClosePriceInUsd(String instrument);
    String getBrokerName();
}
