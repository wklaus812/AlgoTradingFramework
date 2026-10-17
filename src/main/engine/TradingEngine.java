package main.engine;

import main.broker.Broker;
import main.data.Database;
import main.risk.PositionSizer;
import org.ta4j.core.*;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.utils.BarSeriesUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class TradingEngine {
    private Broker broker;
    private Strategy strategy;
    private String instrumentName;
    private BarSeries series;

    public TradingEngine(Broker broker, Strategy strategy, String instrumentName, BarSeries series) {
        this.broker = broker;
        this.strategy = strategy;
        this.instrumentName = instrumentName;
        this.series = series;
    }

    // Gets the latest bar, adds it to the series, and runs the strategy
    public void run() {
        // Get current account balance and latest bar
        BigDecimal accountBalance = broker.getAccountBalance();
        if (accountBalance.doubleValue() < 10) return;  // Don't trade if account balance is < $10
        getLatestbar();
        Bar newBar = series.getLastBar();

        // Check entry and exit conditions
        int endIndex = series.getEndIndex();
        if (strategy.shouldEnter(endIndex) && Database.getOpenTradeId(broker.getBrokerName(), instrumentName) == null) {
            enterTrade(newBar, endIndex);
        } else if (strategy.shouldExit(endIndex) && Database.getOpenTradeId(broker.getBrokerName(), instrumentName) != null) {
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
            System.out.println("Bar added to " + instrumentName + ", close price = " + newBar.getClosePrice().doubleValue());
            series.addBar(newBar);
        } else {
            BarSeriesUtils.replaceBarIfChanged(series, newBar);
            System.out.println("Last bar replaced for " + instrumentName + ", close price = " + newBar.getClosePrice().doubleValue());
        }
    }

    private void enterTrade(Bar newBar, int endIndex) {
        // Place market order with Broker
        // Determine ATR, stop loss, and take profit
        double stopLossAtrMultiplier = 1.5;
        double takeProfitAtrMultiplier = 3;

        ATRIndicator atr = new ATRIndicator(series, 14);
        double atrValue = atr.getValue(series.getEndIndex()).doubleValue();
        double stopLossDistance = atrValue * stopLossAtrMultiplier;
        double takeProfit = atrValue * takeProfitAtrMultiplier + series.getLastBar().getClosePrice().doubleValue();

        // Position size = (Account size * risk %) / (stop distance in pips * pipValue per unit)
        long tradeSize = PositionSizer.calculateUnits(
                broker.getAccountBalance(),                  // Account Balance
                new BigDecimal("0.01"),                 // Risk percentage
                new BigDecimal(stopLossDistance),           // Stop Loss Distance
                instrumentName,                             // Currency Pair as String (e.g. EUR_USD)
                "USD",                                      // Account currency
                broker.getClosePriceInUsd(instrumentName)   // Latest close price in USD
        );
        if (tradeSize == 0) return;

        // Stop Loss and Take Profit details
        OrderDetails orderDetails = new OrderDetails();
        orderDetails.setStopLoss(getRoundedValue(instrumentName, new BigDecimal(stopLossDistance)), true);
        orderDetails.setTakeProfitPrice(getRoundedValue(instrumentName, new BigDecimal(takeProfit)));

        // Place trade and log if successful
        FilledTradeDetails fillDetails = broker.placeMarketOrder(instrumentName, Math.round(tradeSize), orderDetails);
        if (fillDetails != null) {
            Database.insertOpenTrade(broker.getBrokerName(), fillDetails.getTradeId(), instrumentName, fillDetails.getFillPrice().doubleValue(),
                    fillDetails.getFillDate().toString(), fillDetails.getUnits().doubleValue());

            // **** Move this elsewhere
            // Print Trade Details
            System.out.println("======= Trade Details =======");
            System.out.println("    Instrument: " + instrumentName);
            System.out.println("    Price: " + fillDetails.getFillPrice().doubleValue());
            System.out.println("    Trade Size: " + fillDetails.getUnits().doubleValue());
            System.out.println("    Stop Loss: " + orderDetails.getStopLossDistance());
            System.out.println("    Take Profit: " + orderDetails.getTakeProfitPrice());
            System.out.println("=============================");
            // ****
        }

    }

    private void closePosition(Bar newBar, int endIndex) {
        // Close trade and log if successful
        String tradeId = Database.getOpenTradeId(broker.getBrokerName(), instrumentName);
        FilledTradeDetails fillDetails = broker.closePosition(instrumentName, tradeId);
        if (fillDetails != null) {
            Database.closeOpenTrade(tradeId, broker.getBrokerName(), fillDetails.getFillPrice().doubleValue(), fillDetails.getFillDate().toString());

            // **** Move this elsewhere
            // Print Trade Details
            System.out.println("======= Trade Closed =======");
            System.out.println("    Instrument: " + instrumentName);
//            System.out.println("    Close Price: " + closedPosition.getExit().getNetPrice());
//            System.out.println("    Trade Size: " + closedPosition.getExit().getAmount().doubleValue());
//            System.out.println("    Gross Profit: " + closedPosition.getGrossProfit());
//            System.out.println("    Net Profit: " + closedPosition.getProfit());
//            System.out.println("    Gross Return: " + closedPosition.getGrossReturn());
            System.out.println("=============================");
            // ****
        }
    }

    // Gets a rounded value based on the instrument
    private static BigDecimal getRoundedValue(String instrument, BigDecimal value) {
        BigDecimal roundedValue;

        if (instrument.contains("JPY")) {
            roundedValue = value.setScale(3, RoundingMode.HALF_UP);
        } else {
            roundedValue = value.setScale(5, RoundingMode.HALF_UP);
        }

        return roundedValue;
    }
}

