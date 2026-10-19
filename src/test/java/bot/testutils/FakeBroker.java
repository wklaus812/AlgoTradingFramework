package bot.testutils;

import bot.broker.Broker;
import bot.broker.FilledTradeDetails;
import bot.broker.OrderDetails;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import java.math.BigDecimal;
import java.time.Instant;

public class FakeBroker implements Broker {

    private BigDecimal balance = new BigDecimal("10000"); // Default balance is $10,000
    private BigDecimal conversionRate = BigDecimal.ONE;
    private Bar nextBar;
    private FilledTradeDetails fakeMarketOrderFillDetails = new FilledTradeDetails("00001", new BigDecimal("1"), Instant.now(), new BigDecimal("10"));

    public FakeBroker(Bar nextBar) {
        this.nextBar = nextBar;
    }

    @Override
    public BigDecimal getAccountBalance() {
        return balance;
    }

    public void withBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public BarSeries getHistoricalBarSeries(String instrument) {
        return null;
    }

    @Override
    public Bar getLatestBar(String instrument) {
        return nextBar;
    }

    public void withNextBar(Bar nextBar) {
        this.nextBar = nextBar;
    }

    @Override
    public FilledTradeDetails placeMarketOrder(String instrument, int tradeSize, OrderDetails details) {
        return fakeMarketOrderFillDetails;
    }

    public void withMarketOrderFilledTradeDetails(String tradeId, BigDecimal fillPrice, Instant fillDate, BigDecimal units) {
        fakeMarketOrderFillDetails = new FilledTradeDetails(tradeId, fillPrice, fillDate, units);
    }

    @Override
    public FilledTradeDetails closePosition(String instrument, String tradeId) {
        return null;
    }

    @Override
    public BigDecimal getClosePriceInUsd(String instrument) {
        return null;
    }

    public void withUsdConversion(BigDecimal conversionRate) {
        this.conversionRate = conversionRate;
    }

    @Override
    public String getBrokerName() {
        return "fake";
    }
}
