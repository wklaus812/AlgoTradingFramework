package bot.engine;

import bot.broker.Broker;
import bot.risk.PositionSizer;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.transaction.TransactionID;
import org.ta4j.core.*;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.utils.BarSeriesUtils;

import java.awt.event.WindowStateListener;
import java.math.BigDecimal;

public class TradingEngine<T, S> {
    // There will be an instance of a Trading Engine for each bar series / instrument
    // The engine will handle all the trading for the instrument
    // Bring in the OandaBroker and strategy via a constructor
    // Store the bar series in the enginee
    // Transactions should probably be stored in SQLite, but initially, will store them in each engine as a map.
    //      No reason to keep it at a higher level for our purposes right now.
    // For now, the length of the candle, for all strategies, will be defined in the main method
    // Trading engine doesn't care about the candle length, it runs what is provided

    private Broker<T, S> broker;
    private Strategy strategy;
    private S instrumentName;
    private BarSeries series;
    private TradingRecord tradingRecord;

    // Constructor
    // - Broker
    // - Strategy
    // - Instrument Name
    // - BarSeries
    // - Trading Record
    public TradingEngine(Broker<T, S> broker, Strategy strategy, S instrumentName, BarSeries series, TradingRecord tradingRecord) {
        this.broker = broker;
        this.strategy = strategy;
        this.instrumentName = instrumentName;
        this.series = series;
        this.tradingRecord = tradingRecord;
    }

    // Excecute method
    // - Gets the latest bar, adds it to the series, and runs the strategy
    public void run() {
        // Get current account balance and latest bar
        BigDecimal accountBalance = broker.getAccountBalance();
        getLatestbar();
        Bar newBar = series.getLastBar();

        // Check entry and exit conditions
        int endIndex = series.getEndIndex();
        if (strategy.shouldEnter(endIndex) /* Add condition to ensure there is not an open trade with this instrument,
                this is where you would query the sql table */) {
            enterTrade(newBar, endIndex);
        } else if (strategy.shouldExit(endIndex) /* Add condition to ensure there is an open trade with this instrument,
                this is where you would query the sql table */) {
            closePosition();
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

        // Place trade
        T tradeId = broker.placeMarketOrder(instrumentName, Math.round(tradeSize), orderDetails);
        // Need to store trade id in sql server

        // Only enter if market order is successful
        boolean entered = tradingRecord.enter(endIndex, newBar.getClosePrice(), DecimalNum.valueOf(100));
        if (entered) {
            Trade entry = tradingRecord.getLastEntry();

            // **** Move this elsewhere
            // Print Trade Details
            System.out.println("======= Trade Details =======");
            System.out.println("    Instrument: " + instrumentName);
            System.out.println("    Price: " + entry.getAmount().doubleValue());
            System.out.println("    Trade size: " + tradeSize);
            System.out.println("    Stop Loss: " + orderDetails.getStopLossDistance());
            System.out.println("    Take Profit: " + orderDetails.getTakeProfitPrice());
            System.out.println("=============================");
            // ****
        }

    }

    private void closePosition(Bar newBar, int endIndex) {
        // **** Move this elsewhere
        System.out.println("Strategy should EXIT: " + instrumentName);
        // ****

        // Close order with Oanda
        broker.closePosition(instrumentName, null /* Placeholder for now, need to pull from the sql database */);

        boolean exited = tradingRecord.exit(endIndex, newBar.getClosePrice(), DecimalNum.valueOf(10));
        if (exited) {
            Trade exit = tradingRecord.getLastExit();
            System.out.println("Exited on " + exit.getIndex() + " (price=" + exit.getNetPrice().doubleValue()
                    + ", amount=" + exit.getAmount().doubleValue() + ")");
        }

        // Log that the trade was closed somewhere
        // transactionIdsByInstrument.put(instrumentName, null);
    }
}

