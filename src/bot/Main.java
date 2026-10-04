package bot;

import bot.broker.OandaBroker;
import bot.config.Config;
import bot.data.Database;
import bot.engine.TradingEngine;

import bot.strategy.EmaCrossStrategyFactory;
import org.ta4j.core.*;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws Exception {
        Database.connect();
        System.out.println("********************** Initialization **********************");

        // Build historical bar series for forex instruments and build trading engines for each
        HashSet<String> forexInstrumentNames = Config.getForexInstrumentNames();
        HashMap<String, BarSeries> barSeriesByInstrumentName = initAllForexMovingBarSeries(300, forexInstrumentNames);
        TradingRecord tradingRecord = new BaseTradingRecord();
        List<TradingEngine> tradingEngines = initializeTradingEngines(barSeriesByInstrumentName, tradingRecord);

        // Run for the next 50 bars
        for (int i = 0; i < 50; i++) {
            // Thread.sleep(getSleepTime()); This is how it should run for production, also included at the bottom for testing purposes. Exactly 1 should be uncommented at all times
            // Loop over each Trading Engine
            for (TradingEngine engine : tradingEngines) {
                engine.run();
            }
            Thread.sleep(getSleepTime());
        }
    }

    // Builds a map of all forex instruments, getting the number of historical bars requested
    private static HashMap<String, BarSeries> initAllForexMovingBarSeries(int maxBarCount, Set<String> instruments) {
        HashMap<String, BarSeries> barSeriesByInstrumentName = new HashMap<>();
        OandaBroker oanda = new OandaBroker();

        for (String instrument : instruments) {
            BarSeries series = oanda.getHistoricalBarSeries(instrument);
            System.out.print("Initial bar count (" + instrument +"): " + series.getBarCount());

            series.setMaximumBarCount(maxBarCount);
            Num lastBarClosePrice = series.getLastBar().getClosePrice();
            System.out.println(" (limited to " + maxBarCount + "), close price = " + lastBarClosePrice);
            barSeriesByInstrumentName.put(instrument, series);
        }

        return barSeriesByInstrumentName;
    }

    // Build Trading Engines
    private static List<TradingEngine> initializeTradingEngines(HashMap<String, BarSeries> barSeriesByInstrumentName, TradingRecord tradingRecord) {
        List<TradingEngine> tradingEngines = new ArrayList<>();

        OandaBroker oandaBroker = new OandaBroker();
        EmaCrossStrategyFactory emaCrossStrategyFactory = new EmaCrossStrategyFactory();
        for (String instrumentName : barSeriesByInstrumentName.keySet()) {
            TradingEngine newEngine = new TradingEngine(
                    oandaBroker,
                    emaCrossStrategyFactory.build(barSeriesByInstrumentName.get(instrumentName)),
                    instrumentName,
                    barSeriesByInstrumentName.get(instrumentName),
                    tradingRecord
            );

            tradingEngines.add(newEngine);
        }
        return tradingEngines;
    }

    // Returns the amount of time to sleep in milliseconds based on the day of the week
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
}