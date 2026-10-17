package test.testutils;

import main.broker.Broker;
import main.engine.FilledTradeDetails;
import main.engine.OrderDetails;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import java.math.BigDecimal;

public class FakeBroker implements Broker {

    private BigDecimal balance;
    private BigDecimal conversionRate;

    public FakeBroker() {}

    @Override
    public BigDecimal getAccountBalance() {
        if (balance != null) return balance;
        return new BigDecimal("10000"); // Default balance is $10,000
    }

    public void withBalance(BigDecimal b) {
        balance = b;
    }

    @Override
    public BarSeries getHistoricalBarSeries(String instrument) {
        return null;
    }

    @Override
    public Bar getLatestBar(String instrument) {
        return null;
    }

    @Override
    public FilledTradeDetails placeMarketOrder(String instrument, int tradeSize, OrderDetails details) {
        return null;
    }

    @Override
    public FilledTradeDetails closePosition(String instrument, String tradeId) {
        return false;
    }

    @Override
    public BigDecimal getClosePriceInUsd(String instrument) {
        return null;
    }

    public void withUsdConversion(BigDecimal c) {
        conversionRate = c;
    }

    @Override
    public String getBrokerName() {
        return "fake";
    }
}
