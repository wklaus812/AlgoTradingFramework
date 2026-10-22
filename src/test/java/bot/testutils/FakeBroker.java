package bot.testutils;

import bot.broker.Broker;
import bot.broker.FilledTradeDetails;
import bot.broker.OrderDetails;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class FakeBroker implements Broker {

    private BigDecimal balance = new BigDecimal("10000"); // Default balance is $10,000
    private Bar nextBar;
    private FilledTradeDetails fakeMarketOrderFillDetails = new FilledTradeDetails("00001", new BigDecimal("1"), Instant.now(), new BigDecimal("10"));
    private FilledTradeDetails fakeClosePositionFillDetails = new FilledTradeDetails("00001", new BigDecimal("1"), Instant.now(), new BigDecimal("10"));
    private boolean rejectNextOrder = false;

    // Used for assertions
    public record PlacedOrder(String instrumentName, int units, OrderDetails orderDetails){}
    public final List<PlacedOrder> placedMarketOrders = new ArrayList<>();
    public final List<String> closedTradeIds = new ArrayList<>();

    public FakeBroker(Bar nextBar) {
        this.nextBar = nextBar;
    }

    // Custom methods
    public void withBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void withNextBar(Bar nextBar) {
        this.nextBar = nextBar;
    }

    public void withMarketOrderFilledTradeDetails(String tradeId, BigDecimal fillPrice, Instant fillDate, BigDecimal units) {
        fakeMarketOrderFillDetails = new FilledTradeDetails(tradeId, fillPrice, fillDate, units);
    }

    public void withClosePositionFilledTradeDetails(String tradeId, BigDecimal fillPrice, Instant fillDate, BigDecimal units) {
        fakeClosePositionFillDetails = new FilledTradeDetails(tradeId, fillPrice, fillDate, units);
    }

    public void setRejectNextOrder(boolean rejectNextOrder) {
        this.rejectNextOrder = rejectNextOrder;
    }

    // Override methods
    @Override public BigDecimal getAccountBalance() {
        return balance;
    }
    @Override public Bar getLatestBar(String instrument) {
        return nextBar;
    }
    @Override public BarSeries getHistoricalBarSeries(String instrument) {
        return null;
    }
    @Override public BigDecimal getClosePriceInUsd(String instrument) {
        return BigDecimal.ONE;
    }
    @Override public String getBrokerName() {
        return "fake";
    }

    @Override
    public FilledTradeDetails placeMarketOrder(String instrument, int tradeSize, OrderDetails details) {
        placedMarketOrders.add(new PlacedOrder(instrument, tradeSize, details));
        if (rejectNextOrder) return null;       // Log that the order was requested, but "reject" by returning null
        return fakeMarketOrderFillDetails;
    }

    @Override
    public FilledTradeDetails closePosition(String instrument, String tradeId) {
        closedTradeIds.add(tradeId);
        if (rejectNextOrder) return null;       // Log that the order was requested, but "reject" by returning null
        return fakeClosePositionFillDetails;
    }

}
