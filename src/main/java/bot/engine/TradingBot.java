package bot.engine;

import bot.broker.Broker;
import bot.broker.TradeStatusDetails;
import bot.data.Database;
import bot.strategy.StrategyFactory;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.Num;

import java.util.*;

public class TradingBot {
    private final Broker broker;
    private final StrategyFactory strategyFactory;
    private final Set<String> instruments;
    private final int historicalBarCount = 300;

    public TradingBot(Broker broker, StrategyFactory strategyFactory, Set<String> instruments) {
        this.broker = broker;
        this.strategyFactory = strategyFactory;
        this.instruments = instruments;
    }

    public void run() {
        reconcileDatabaseWithBroker(broker);

        // Build historical bar series for forex instruments and build trading engines for each
        HashMap<String, BarSeries> barSeriesByInstrumentName = initAllForexMovingBarSeries(broker);
        List<TradingEngine> tradingEngines = initializeTradingEngines(broker, barSeriesByInstrumentName);

        // Loop over each Trading Engine
        for (TradingEngine engine : tradingEngines) {
            engine.run();
        }
    }

    private void reconcileDatabaseWithBroker(Broker broker) {
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
            try {
                TradeStatusDetails details = broker.getTradeStatus(tradeId);
                if (details.getTradeStatus() == TradeStatusDetails.Status.CLOSED) {
                    tradeStatusDetailsList.add(details);
                }
            } catch (Exception e) {
                // Log in SL4J, that there was a failure getting trade status
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
    private HashMap<String, BarSeries> initAllForexMovingBarSeries(Broker broker) {
        HashMap<String, BarSeries> barSeriesByInstrumentName = new HashMap<>();

        for (String instrument : instruments) {
            BarSeries series = broker.getHistoricalBarSeries(instrument);
            System.out.print("Initial bar count (" + instrument +"): " + series.getBarCount());

            series.setMaximumBarCount(historicalBarCount);
            Num lastBarClosePrice = series.getLastBar().getClosePrice();
            System.out.println(" (limited to " + historicalBarCount + "), close price = " + lastBarClosePrice);
            barSeriesByInstrumentName.put(instrument, series);
        }

        return barSeriesByInstrumentName;
    }

    // Build Trading Engines
    private List<TradingEngine> initializeTradingEngines(Broker broker, HashMap<String, BarSeries> barSeriesByInstrumentName) {
        List<TradingEngine> tradingEngines = new ArrayList<>();

        for (String instrumentName : barSeriesByInstrumentName.keySet()) {
            TradingEngine newEngine = new TradingEngine(
                    broker,
                    strategyFactory.build(barSeriesByInstrumentName.get(instrumentName)),
                    instrumentName,
                    barSeriesByInstrumentName.get(instrumentName)
            );

            tradingEngines.add(newEngine);
        }
        return tradingEngines;
    }

}
