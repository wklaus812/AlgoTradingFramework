import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.transaction.TransactionID;
import org.ta4j.core.*;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.StopGainRule;
import org.ta4j.core.rules.TrailingStopLossRule;

import java.util.HashMap;
import java.util.Set;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    private static HashMap<InstrumentName, BarSeries> initAllMovingBarSeries(int maxBarCount, Set<InstrumentName> instruments) throws ExecuteException, RequestException, InterruptedException {
        HashMap<InstrumentName, BarSeries> barSeriesByInstrumentName = new HashMap<InstrumentName, BarSeries>();
        OandaInterface oanda = new OandaInterface();

        for (InstrumentName instrument : instruments) {
            BarSeries series = oanda.getBarSeriesFromOanda(instrument);
            System.out.print("Initial bar count (" + instrument.toString() +"): " + series.getBarCount());

            series.setMaximumBarCount(maxBarCount);
            Num lastBarClosePrice = series.getLastBar().getClosePrice();
            System.out.println(" (limited to " + maxBarCount + "), close price = " + lastBarClosePrice);
            barSeriesByInstrumentName.put(instrument, series);
            Thread.sleep(100);
        }


        return barSeriesByInstrumentName;
    }


    private static HashMap<InstrumentName, BaseStrategy> buildStrategyMap(HashMap<InstrumentName, BarSeries> barSeriesByInstrumentName) {
        HashMap<InstrumentName, BaseStrategy> strategiesByInstrumentName = new HashMap<>();

        for (InstrumentName instrument : barSeriesByInstrumentName.keySet()) {
            BarSeries series = barSeriesByInstrumentName.get(instrument);

            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            EMAIndicator ema = new EMAIndicator(closePrice, 200);
            Rule buyingRule = new CrossedUpIndicatorRule(ema, closePrice);
            Rule sellingRule = new CrossedDownIndicatorRule(ema, closePrice)
                    .or(new TrailingStopLossRule(closePrice, series.numFactory().numOf(4)))     // Determine using average true range
                    .or(new StopGainRule(closePrice, series.numFactory().numOf(3)));            // Determine using average true range
            strategiesByInstrumentName.put(instrument, new BaseStrategy(buyingRule, sellingRule));

            if (barSeriesByInstrumentName.get(instrument) == null) {
                throw new IllegalArgumentException("Series cannot be null");
            }
        }

        return strategiesByInstrumentName;
    }


    public static void main(String[] args) throws ExecuteException, RequestException, InterruptedException {

        System.out.println("********************** Initialization **********************");
        // Need to build a map of TradeIds by instrument. If a position is opened, store the id in the map, if a position is closed
        // remove the Id from the map. Only close a position if there is trade id is blank for an instrument. Only open a position if trade id is not blank
        // for an instrument
        HashMap<InstrumentName, TransactionID> transactionIdsByInstrument = Config.getTransactionIdsByInstrumentName();
        
        // Get inital Bar Series
        // Neet to create a map of Bar Series by Instrument
        HashMap<InstrumentName, BarSeries> barSeriesByInstrumentName = initAllMovingBarSeries(300, transactionIdsByInstrument.keySet());

        // Build the trading strategy
        // Build the strategy for each Instrument
        HashMap<InstrumentName, BaseStrategy> strategiesByInstrumentName = buildStrategyMap(barSeriesByInstrumentName);

        // Initialize the trading history
        TradingRecord tradingRecord = new BaseTradingRecord();
        System.out.println("************************************************************");

        OandaInterface oanda = new OandaInterface();

        // Run the strategy for the next 50 bars
        for (int i = 0; i < 1; i++) {

            // Loop over each instrument
            for (InstrumentName instrumentName : transactionIdsByInstrument.keySet()) {
                BarSeries series = barSeriesByInstrumentName.get(instrumentName);
                BaseStrategy strategy = strategiesByInstrumentName.get(instrumentName);

                // Get new bar and add to series
                Bar newBar = oanda.getLatestBar(instrumentName);
                if (series.getLastBar().equals(newBar)) {
                    System.out.println("EQUAL, DO NOTHING: " + instrumentName.toString());
                    continue;
                }
                System.out.println("------------------------------------------------------\n" + "Bar " + i
                        + " added for " + instrumentName.toString() + ", close price = " + newBar.getClosePrice().doubleValue());
                series.addBar(newBar);

                // Check entry and exit conditions
                int endIndex = series.getEndIndex();
                if (strategy.shouldEnter(endIndex) && transactionIdsByInstrument.get(instrumentName) == null) {
                    System.out.println("Strategy should ENTER on " + endIndex);

                    // Place market order with Oanda
                    TransactionID tradeId = oanda.placeMarketOrder(instrumentName);
                    transactionIdsByInstrument.put(instrumentName, tradeId);


                    // Only enter if market order is successful
                    boolean entered = tradingRecord.enter(endIndex, newBar.getClosePrice(), DecimalNum.valueOf(10));
                    if (entered) {
                        Trade entry = tradingRecord.getLastEntry();
                        System.out.println("Entered on " + entry.getIndex() + " (price=" + entry.getNetPrice().doubleValue()
                                + ", amount=" + entry.getAmount().doubleValue() + ")");
                    }

                } else if (strategy.shouldExit(endIndex) && transactionIdsByInstrument.get(instrumentName) != null) {
                    System.out.println("Strategy should EXIT on " + endIndex);

                    // Close order with Oanda
                    oanda.closePosition(instrumentName, transactionIdsByInstrument.get(instrumentName));

                    boolean exited = tradingRecord.exit(endIndex, newBar.getClosePrice(), DecimalNum.valueOf(10));
                    if (exited) {
                        Trade exit = tradingRecord.getLastExit();
                        System.out.println("Exited on " + exit.getIndex() + " (price=" + exit.getNetPrice().doubleValue()
                                + ", amount=" + exit.getAmount().doubleValue() + ")");
                    }

                    transactionIdsByInstrument.put(instrumentName, null);

                }
            }



            Thread.sleep(100); // Probably should dynamically calculate this to the specific time we want to check again
        }
    }


}