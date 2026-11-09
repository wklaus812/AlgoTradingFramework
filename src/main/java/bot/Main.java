package bot;

import bot.broker.OandaBroker;
import bot.broker.Broker;
import bot.broker.TradeStatusDetails;
import bot.data.Database;
import bot.engine.TradingEngine;

import bot.strategy.EmaCrossStrategyFactory;
import org.ta4j.core.*;
import org.ta4j.core.num.Num;

import javax.xml.crypto.Data;
import java.util.*;

public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("********************** Initialization **********************");
        Database.connect();
        OandaBroker oanda = new OandaBroker();
        reconcileDatabaseWithBroker(oanda);

        // Build historical bar series for forex instruments and build trading engines for each
        HashSet<String> forexInstrumentNames = Config.getForexInstrumentNames();
        HashMap<String, BarSeries> barSeriesByInstrumentName = initAllForexMovingBarSeries(oanda, 300, forexInstrumentNames);
        List<TradingEngine> tradingEngines = initializeTradingEngines(barSeriesByInstrumentName);

        // Loop over each Trading Engine
        for (TradingEngine engine : tradingEngines) {
            engine.run();
        }
    }

    private static void reconcileDatabaseWithBroker(Broker broker) {
        // Get a list of open trade ids from the database, for provided broker
        List<String> openTradeIds = Database.getOpenTradeIds(broker.getBrokerName());
        if (openTradeIds == null) {
            // This is temporary, add a database exception that gets thrown
            System.out.println("Error getting open tradeIds");
            return;
        }
        List<TradeStatusDetails> tradeStatusDetailsList = new ArrayList<>();

        // Get the status of each trade from Oanda
        for (String tradeId : openTradeIds) {
            TradeStatusDetails details = broker.getTradeStatus(tradeId);
            if (details.getTradeStatus() == TradeStatusDetails.Status.CLOSED) {
                tradeStatusDetailsList.add(details);
            }
        }

        // Update the database with the closed trades
        // Calling separate update transactions for each trade is not great practice and building a custom method to update
        // in batch would be better, but in the bots current state we are limited to a maximum 28 possible open trades at a
        // time, so this is ok for now
        for (TradeStatusDetails details : tradeStatusDetailsList) {
            Database.closeOpenTrade(
                    details.getFilledTradeDetails().getTradeId(),
                    broker.getBrokerName(),
                    details.getFilledTradeDetails().getFillPrice().doubleValue(),
                    details.getFilledTradeDetails().getFillDate().toString(),
                    details.getCloseReason()
                    );
        }

    }

    // Builds a map of all forex instruments, getting the number of historical bars requested
    private static HashMap<String, BarSeries> initAllForexMovingBarSeries(Broker broker, int maxBarCount, Set<String> instruments) {
        HashMap<String, BarSeries> barSeriesByInstrumentName = new HashMap<>();

        for (String instrument : instruments) {
            BarSeries series = broker.getHistoricalBarSeries(instrument);
            System.out.print("Initial bar count (" + instrument +"): " + series.getBarCount());

            series.setMaximumBarCount(maxBarCount);
            Num lastBarClosePrice = series.getLastBar().getClosePrice();
            System.out.println(" (limited to " + maxBarCount + "), close price = " + lastBarClosePrice);
            barSeriesByInstrumentName.put(instrument, series);
        }

        return barSeriesByInstrumentName;
    }

    // Build Trading Engines
    private static List<TradingEngine> initializeTradingEngines(HashMap<String, BarSeries> barSeriesByInstrumentName) {
        List<TradingEngine> tradingEngines = new ArrayList<>();

        OandaBroker oandaBroker = new OandaBroker();
        EmaCrossStrategyFactory emaCrossStrategyFactory = new EmaCrossStrategyFactory();
        for (String instrumentName : barSeriesByInstrumentName.keySet()) {
            TradingEngine newEngine = new TradingEngine(
                    oandaBroker,
                    emaCrossStrategyFactory.build(barSeriesByInstrumentName.get(instrumentName)),
                    instrumentName,
                    barSeriesByInstrumentName.get(instrumentName)
            );

            tradingEngines.add(newEngine);
        }
        return tradingEngines;
    }

    // Code below is deprecated, will fully delete at a later date once docker is set up

    // Returns the amount of time to sleep in milliseconds based on the day of the week
//    private static long getSleepTime() {
//        // Get the current time
//        Instant currentTime = Instant.now();
//        LocalDateTime currentDateTime = LocalDateTime.now(ZoneId.of("America/Chicago"));
//        LocalDateTime targetDateTime = null;
//
//        int daysToAdd = getDaysToAdd(currentDateTime);
//
//        targetDateTime = LocalDateTime.of(
//                currentDateTime.getYear(),
//                currentDateTime.getMonthValue(),
//                currentDateTime.getDayOfMonth(),
//                15,
//                45,
//                0
//        ).plusDays(daysToAdd);
//        // Convert the target to an Instant, considering the system's default time zone
//        Instant targetInstant = targetDateTime.atZone(ZoneId.of("America/Chicago")).toInstant();
//
//        // Calculate the duration between the two instants
//        Duration duration = Duration.between(currentTime, targetInstant);
//        System.out.println("Sleeping until: " + targetInstant.atZone(ZoneId.of("America/Chicago")).toString());
//
//        // Return the difference in milliseconds
//        return duration.toMillis();
//    }

//    private static int getDaysToAdd(LocalDateTime currentDateTime) {
//        int daysToAdd;
//
//        // Same day and not Saturday/Sunday, sleep until afternoon
//        // Friday, sleep 3 days
//        // Saturday, sleep 2 days
//        // Otherwise sleep 1 day
//
//        if (currentDateTime.getHour() < 15 && currentDateTime.getMinute() < 45 && currentDateTime.getDayOfWeek() != DayOfWeek.SATURDAY && currentDateTime.getDayOfWeek() != DayOfWeek.SUNDAY ) {
//            daysToAdd = 0;
//        } else if (currentDateTime.getDayOfWeek() == DayOfWeek.FRIDAY) {
//            daysToAdd = 3;
//        } else if (currentDateTime.getDayOfWeek() == DayOfWeek.SATURDAY) {
//            daysToAdd = 2;
//        } else {
//            daysToAdd = 1;
//        }
//        return daysToAdd;
//    }
}