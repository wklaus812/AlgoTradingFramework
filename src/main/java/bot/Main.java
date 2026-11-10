package bot;

import bot.broker.OandaBroker;
import bot.data.Database;
import bot.engine.TradingBot;
import bot.strategy.EmaCrossStrategyFactory;

public class Main {
    public static void main(String[] args) {
        Database.connect();
        new TradingBot(new OandaBroker(), new EmaCrossStrategyFactory(), Config.getForexInstrumentNames()).run();
    }
}