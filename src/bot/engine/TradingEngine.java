package bot.engine;

import bot.broker.Broker;
import bot.data.Database;
import bot.risk.PositionSizer;
import org.ta4j.core.*;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.utils.BarSeriesUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradingEngine<T, S> {
    private Broker<T, S> broker;
    private Strategy strategy;
    private S instrumentName;
    private BarSeries series;
    private TradingRecord tradingRecord;

    public TradingEngine(Broker<T, S> broker, Strategy strategy, S instrumentName, BarSeries series, TradingRecord tradingRecord) {
        this.broker = broker;
        this.strategy = strategy;
        this.instrumentName = instrumentName;
        this.series = series;
        this.tradingRecord = tradingRecord;
    }

    // Gets the latest bar, adds it to the series, and runs the strategy
    public void run() {
        // Get current account balance and latest bar
        BigDecimal accountBalance = broker.getAccountBalance();
        getLatestbar();
        Bar newBar = series.getLastBar();

        // Check entry and exit conditions
        int endIndex = series.getEndIndex();
        if (strategy.shouldEnter(endIndex) && Database.getOpenTradeId(broker.getBrokerName(), instrumentName.toString()) == null) {
            enterTrade(newBar, endIndex);
        } else if (strategy.shouldExit(endIndex) && Database.getOpenTradeId(broker.getBrokerName(), instrumentName.toString()) != null) {
            closePosition(newBar, endIndex);
        } else {
            // No trade
            System.out.println("No trade: " + instrumentName);
        }
    }

    // Gets the most recent bar from the broker
    private void getLatestbar() {
        // Get new bar and add to series or update if a bar already exists for time frame
        Bar newBar = broker.getLatestBar(instrumentName);
        if (!series.getLastBar().getEndTime().equals(newBar.getEndTime())) {
            System.out.println("Bar added to " + instrumentName.toString() + ", close price = " + newBar.getClosePrice().doubleValue());
            series.addBar(newBar);
        } else {
            BarSeriesUtils.replaceBarIfChanged(series, newBar);
            System.out.println("Last bar replaced for " + instrumentName.toString() + ", close price = " + newBar.getClosePrice().doubleValue());
        }
    }

    private void enterTrade(Bar newBar, int endIndex) {
        // Place market order with Broker
        // Position size = (Account size * risk %) / (stop distance in pips * pipValue per unit)
        long tradeSize = PositionSizer.calculateUnits(
                broker.getAccountBalance(),                  // Account Balance
                new BigDecimal("0.01"),                 // Risk percentage
                series,                                     // Bar Series
                14,                                         // ATR Period
                new BigDecimal("1.5"),                  // ATR Multiplier
                instrumentName.toString(),                  // Currency Pair as String (e.g. EUR_USD)
                "USD",                                      // Account currency
                broker.getClosePriceInUsd(instrumentName)   // Latest close price in USD
        );

        // Determine ATR, stop loss, and take profit
        ATRIndicator atr = new ATRIndicator(series, 14);
        double atrValue= atr.getValue(series.getEndIndex()).doubleValue();
        double stopLoss = atrValue * 1.5;
        double takeProfit = atrValue * 3 + series.getLastBar().getClosePrice().doubleValue();

        // Stop Loss and Take Profit details
        OrderDetails orderDetails = new OrderDetails();
        orderDetails.setStopLoss(new BigDecimal(stopLoss), true);
        orderDetails.setTakeProfitPrice(new BigDecimal(takeProfit));

        // Place trade and log if successful
        T tradeId = broker.placeMarketOrder(instrumentName, Math.round(tradeSize), orderDetails);
        if (tradeId != null) {
            tradingRecord.enter(endIndex, newBar.getClosePrice(), DecimalNum.valueOf(100));
            Trade entry = tradingRecord.getLastEntry();
            Database.insertOpenTrade(broker.getBrokerName(), tradeId.toString(), instrumentName.toString(), entry.getAmount().doubleValue(), LocalDateTime.now().toString(), tradeSize);

            // **** Move this elsewhere
            // Print Trade Details
            System.out.println("======= Trade Details =======");
            System.out.println("    Instrument: " + instrumentName);
            System.out.println("    Price: " + entry.getAmount().doubleValue());
            System.out.println("    Trade Size: " + tradeSize);
            System.out.println("    Stop Loss: " + orderDetails.getStopLossDistance());
            System.out.println("    Take Profit: " + orderDetails.getTakeProfitPrice());
            System.out.println("=============================");
            // ****
        }

    }

    private void closePosition(Bar newBar, int endIndex) {
        // Close trade and log if successful
        String tradeId = Database.getOpenTradeId(broker.getBrokerName(), instrumentName.toString());
        boolean tradeClosed = broker.closePosition(instrumentName, tradeId);
        if (tradeClosed) {
            tradingRecord.exit(endIndex, newBar.getClosePrice(), DecimalNum.valueOf(10));
            Position closedPosition = tradingRecord.getLastPosition();
            Database.closeOpenTrade(tradeId, broker.getBrokerName(), newBar.getClosePrice().doubleValue(), LocalDateTime.now().toString());

            // **** Move this elsewhere
            // Print Trade Details
            System.out.println("======= Trade Closed =======");
            System.out.println("    Instrument: " + instrumentName);
            System.out.println("    Close Price: " + closedPosition.getExit().getNetPrice());
            System.out.println("    Trade Size: " + closedPosition.getExit().getAmount().doubleValue());
            System.out.println("    Gross Profit: " + closedPosition.getGrossProfit());
            System.out.println("    Net Profit: " + closedPosition.getProfit());
            System.out.println("    Gross Return: " + closedPosition.getGrossReturn());
            System.out.println("=============================");
            // ****
        }
    }
}

