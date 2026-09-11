package bot;

import bot.broker.OandaBroker;
import bot.data.CurrencyConverter;
import bot.config.Config;
import bot.data.Database;
import bot.engine.OrderDetails;
import bot.engine.TradingEngine;
import bot.risk.PositionSizer;

import bot.strategy.EmaCrossStrategyFactory;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.transaction.TransactionID;
import org.ta4j.core.*;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.StopGainRule;
import org.ta4j.core.rules.TrailingStopLossRule;
import org.ta4j.core.utils.BarSeriesUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

public class Main {

    private static HashMap<InstrumentName, BarSeries> initAllMovingBarSeries(int maxBarCount, Set<InstrumentName> instruments) throws ExecuteException, RequestException, InterruptedException {
        HashMap<InstrumentName, BarSeries> barSeriesByInstrumentName = new HashMap<InstrumentName, BarSeries>();
        OandaBroker oanda = new OandaBroker();

        for (InstrumentName instrument : instruments) {
            BarSeries series = oanda.getHistoricalBarSeries(instrument);
            System.out.print("Initial bar count (" + instrument.toString() +"): " + series.getBarCount());

            series.setMaximumBarCount(maxBarCount);
            Num lastBarClosePrice = series.getLastBar().getClosePrice();
            System.out.println(" (limited to " + maxBarCount + "), close price = " + lastBarClosePrice);
            barSeriesByInstrumentName.put(instrument, series);
            //Thread.sleep(10);
        }

        return barSeriesByInstrumentName;
    }

    private static HashMap<InstrumentName, Strategy> buildStrategyMap(HashMap<InstrumentName, BarSeries> barSeriesByInstrumentName) {
        HashMap<InstrumentName, Strategy> strategiesByInstrumentName = new HashMap<>();
        EmaCrossStrategyFactory emaCrossStrategyFactory = new EmaCrossStrategyFactory();

        for (InstrumentName instrument : barSeriesByInstrumentName.keySet()) {
            BarSeries series = barSeriesByInstrumentName.get(instrument);

            strategiesByInstrumentName.put(instrument, emaCrossStrategyFactory.build(series));

            if (barSeriesByInstrumentName.get(instrument) == null) {
                throw new IllegalArgumentException("Series cannot be null");
            }
        }

        return strategiesByInstrumentName;
    }

    private static long getSleepTime() {
        // Get the current time
        Instant currentTime = Instant.now();
        LocalDateTime currentDateTime = LocalDateTime.now(ZoneId.of("America/Chicago"));
        LocalDateTime targetDateTime = null;

        int daysToAdd = getDaysToAdd(currentDateTime);

        targetDateTime = LocalDateTime.of(
                currentDateTime.getYear(),
                currentDateTime.getMonthValue(),
                currentDateTime.getDayOfMonth(),
                15,
                45,
                0
        ).plusDays(daysToAdd);
        // Convert the target to an Instant, considering the system's default time zone
        Instant targetInstant = targetDateTime.atZone(ZoneId.of("America/Chicago")).toInstant();

        // Calculate the duration between the two instants
        Duration duration = Duration.between(currentTime, targetInstant);
        System.out.println("Sleeping until: " + targetInstant.atZone(ZoneId.of("America/Chicago")).toString());

        // Return the difference in milliseconds
        return duration.toMillis();
    }

    private static int getDaysToAdd(LocalDateTime currentDateTime) {
        int daysToAdd;

        // Same day and not Saturday/Sunday, sleep until afternoon
        // Friday, sleep 3 days
        // Saturday, sleep 2 days
        // Otherwise sleep 1 day

        if (currentDateTime.getHour() < 15 && currentDateTime.getMinute() < 45 && currentDateTime.getDayOfWeek() != DayOfWeek.SATURDAY && currentDateTime.getDayOfWeek() != DayOfWeek.SUNDAY ) {
            daysToAdd = 0;
        } else if (currentDateTime.getDayOfWeek() == DayOfWeek.FRIDAY) {
            daysToAdd = 3;
        } else if (currentDateTime.getDayOfWeek() == DayOfWeek.SATURDAY) {
            daysToAdd = 2;
        } else {
            daysToAdd = 1;
        }
        return daysToAdd;
    }

    // Gets a rounded value based on the instrument
    private static BigDecimal getRoundedValue(InstrumentName instrument, BigDecimal value) {
        BigDecimal roundedValue;

        if (instrument.toString().contains("JPY")) {
            roundedValue = value.setScale(3, RoundingMode.HALF_UP);
        } else {
            roundedValue = value.setScale(5, RoundingMode.HALF_UP);
        }

        return roundedValue;
    }

    // Build Trading Engines
    private static List<TradingEngine<InstrumentName, TransactionID>> initializeTradingEngines() {
        // ...
        return null;
    }

    public static void main(String[] args) throws Exception {
        Database.connect();
        System.out.println("********************** Initialization **********************");
        // Need to build a map of TradeIds by instrument. If a position is opened, store the id in the map, if a position is closed
        // remove the Id from the map. Only close a position if there is trade id is blank for an instrument. Only open a position if trade id is not blank
        // for an instrument
        HashMap<InstrumentName, TransactionID> transactionIdsByInstrument = Config.getTransactionIdsByInstrumentName();     // Move this to a database in the future to preserve state

        // Get inital Bar Series
        // Neet to create a map of Bar Series by Instrument
        HashMap<InstrumentName, BarSeries> barSeriesByInstrumentName = initAllMovingBarSeries(300, transactionIdsByInstrument.keySet());

        // Build the trading strategy
        // Build the strategy for each Instrument
        HashMap<InstrumentName, Strategy> strategiesByInstrumentName = buildStrategyMap(barSeriesByInstrumentName);

        // Initialize the trading history
        TradingRecord tradingRecord = new BaseTradingRecord();

        List<TradingEngine<InstrumentName, TransactionID>> tradingEngines = initializeTradingEngines();

        OandaBroker oanda = new OandaBroker();

        // Run the strategy for the next 50 bars
        for (int i = 0; i < 50; i++) {
            // Loop over each Trading Engine
            for (TradingEngine<InstrumentName, TransactionID> engine : tradingEngines) {
                engine.run();
            }
            Thread.sleep(getSleepTime());
        }
    }
}