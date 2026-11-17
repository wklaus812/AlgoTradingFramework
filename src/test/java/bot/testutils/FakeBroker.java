package bot.testutils;

import bot.broker.*;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FakeBroker implements Broker {

    private BigDecimal balance = new BigDecimal("10000"); // Default balance is $10,000
    private Bar nextBar;
    private FilledTradeDetails fakeMarketOrderFillDetails = new FilledTradeDetails("00001", new BigDecimal("1"), Instant.now(), new BigDecimal("10"));
    private FilledTradeDetails fakeClosePositionFillDetails = new FilledTradeDetails("00001", new BigDecimal("1"), Instant.now(), new BigDecimal("10"));
    private boolean rejectNextOrder = false;
    private BarSeries historicalBarSeries;
    private TradeStatusDetails tradeStatusDetails;
    private final Set<String> failingInstruments = new HashSet<>();

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

    public void setTradeStatusDetails(String tradeId, TradeStatusDetails.Status status, TradeCloseReason reason, FilledTradeDetails fillDetails) {
        this.tradeStatusDetails = new TradeStatusDetails(tradeId, status, reason, fillDetails);
    }

    public void failLatestBarFor(String instrument) {
        failingInstruments.add(instrument);
    }

    // Override methods
    @Override
    public BigDecimal getAccountBalance() {
        return balance;
    }

    @Override
    public Bar getLatestBar(String instrument) {
        if (failingInstruments.contains(instrument)) {
            throw new BrokerException("Simulated failure for " + instrument);
        }
        return nextBar;
    }

    @Override
    public BarSeries getHistoricalBarSeries(String instrument) {
        return TestSeries.flat(50, 1.1220, 0.006);
    }

    @Override
    public BigDecimal getClosePriceInUsd(String instrument) {
        return BigDecimal.ONE;
    }

    @Override
    public String getBrokerName() {
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

    @Override
    public TradeStatusDetails getTradeStatus(String tradeId) {
        if (tradeStatusDetails != null) return tradeStatusDetails;
        return new TradeStatusDetails(tradeId, TradeStatusDetails.Status.OPEN); // Default to open
    }

}
