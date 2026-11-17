package bot.engine;

import bot.broker.FilledTradeDetails;
import bot.broker.TradeCloseReason;
import bot.broker.TradeStatusDetails;
import bot.data.Database;
import bot.data.DatabaseException;
import bot.testutils.FakeBroker;
import bot.testutils.TestSeries;
import bot.testutils.TestStrategy;
import bot.testutils.TestStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ta4j.core.BarSeries;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TradingBotTest {

    private String testUrl;
    private TestStrategyFactory testStrategyFactory;
    private TestStrategy testStrategy;
    private BarSeries testSeries;
    private FakeBroker fakeBroker;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Set up database
        String testDbPath = tempDir.resolve("test.db").toString();
        testUrl = "jdbc:sqlite:" + testDbPath;
        Database.connectTestContext(testUrl);

        // Seed variables
        testStrategyFactory = new TestStrategyFactory();
        testStrategy = new TestStrategy();
        testSeries = TestSeries.flat(50, 1.1220, 0.006);
        fakeBroker = new FakeBroker(TestSeries.nextBar(testSeries, 1.1240, 0.003));
        testStrategy.setEnterNext(true);
        testStrategyFactory.setStrategy(testStrategy);
    }

    // Happy path run test
    @Test
    void runTest() {
        // Set filled trade details and run the bot
        fakeBroker.withMarketOrderFilledTradeDetails("00001", new BigDecimal("1.1215"), Instant.now(), new BigDecimal("10"));
        new TradingBot(fakeBroker, testStrategyFactory, Set.of("EUR_USD")).run();

        // Verify the broker received the trades
        assertEquals(1, fakeBroker.placedMarketOrders.size());
        assertEquals("EUR_USD", fakeBroker.placedMarketOrders.getFirst().instrumentName());
        // Verify the trade was logged to the database
        assertEquals("00001", Database.getOpenTradeId("fake", "EUR_USD"));
    }

    // Happy path reconcile test
    @Test
    void reconcileDatabaseWithBrokerTest() {
        // Seed an open trade in the database
        Database.insertOpenTrade("fake", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1);

        // Broker should report this trade is closed when getStatus is called
        fakeBroker.setTradeStatusDetails("00001", TradeStatusDetails.Status.CLOSED, TradeCloseReason.TAKE_PROFIT,
                new FilledTradeDetails("00001",new BigDecimal("1.1249"),
                        Instant.now().minus(3, ChronoUnit.HOURS), new BigDecimal("1")));

        // Set the broker to enter a new trade with the same instrument
        fakeBroker.withMarketOrderFilledTradeDetails("00002", new BigDecimal("1.1215"), Instant.now(), new BigDecimal("10"));

        // Run the bot and ensure that a new trade was placed with the broker and logged to the database successfully
        new TradingBot(fakeBroker, testStrategyFactory, Set.of("EUR_USD")).run();

        // Verify the broker received the trade
        assertEquals(1, fakeBroker.placedMarketOrders.size());
        assertEquals("EUR_USD", fakeBroker.placedMarketOrders.getFirst().instrumentName());
        // Verify the trade was logged to the database and that the old was closed
        assertEquals("00002", Database.getOpenTradeId("fake", "EUR_USD"));
        assertFalse(Database.getOpenTradeIds("fake").contains("00001"));
    }

    // Reconcile database failure, ensure this crashes the bot
    @Test
    void reconcileDatabaseWithBrokerExceptionTest() throws SQLException {
        // Seed an open trade in the database
        Database.insertOpenTrade("fake", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1);

        // Broker should report this trade is closed when getStatus is called
        fakeBroker = new FakeBroker(TestSeries.nextBar(testSeries, 1.1240, 0.003));
        fakeBroker.setTradeStatusDetails("00001", TradeStatusDetails.Status.CLOSED, TradeCloseReason.TAKE_PROFIT,
                new FilledTradeDetails("00001",new BigDecimal("1.1249"),
                        Instant.now().minus(3, ChronoUnit.HOURS), new BigDecimal("1")));

        // Set the broker to enter a new trade with the same instrument
        fakeBroker.withMarketOrderFilledTradeDetails("00002", new BigDecimal("1.1215"), Instant.now(), new BigDecimal("10"));

        // Drop the table, forcing an error when getOpenTradeIds gets called
        try (Connection c = DriverManager.getConnection(testUrl);
             Statement s = c.createStatement()) {
            s.execute("DROP TABLE trades");
        }

        // Verify that an error is thrown, the bot aborts, and no trades were placed
        assertThrows(DatabaseException.class, () -> new TradingBot(fakeBroker, testStrategyFactory, Set.of("EUR_USD")).run());
        assertEquals(0, fakeBroker.placedMarketOrders.size());
    }

    // Catch single instrument failure test
    @Test
    void runContinueAfterEngineErrorTest() {
        // Run the bot for two pairs
        fakeBroker.withMarketOrderFilledTradeDetails("00001", new BigDecimal("1.1215"), Instant.now(), new BigDecimal("10"));
        fakeBroker.failLatestBarFor("EUR_USD");
        new TradingBot(fakeBroker, testStrategyFactory, new LinkedHashSet<>(List.of("EUR_USD", "GBP_USD"))).run();

        // Ensure EUR_USD didn't trade, but GBP_USD did
        assertEquals(1, fakeBroker.placedMarketOrders.size());
        assertEquals("GBP_USD", fakeBroker.placedMarketOrders.getFirst().instrumentName());
        assertNotNull(Database.getOpenTradeId(fakeBroker.getBrokerName(), "GBP_USD"));
        assertNull(Database.getOpenTradeId(fakeBroker.getBrokerName(), "EUR_USD"));
    }
}
