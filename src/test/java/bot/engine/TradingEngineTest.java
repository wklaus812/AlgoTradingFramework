package bot.engine;

import bot.data.Database;
import bot.testutils.FakeBroker;
import bot.testutils.TestStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import bot.testutils.TestSeries;
import org.ta4j.core.BarSeries;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class TradingEngineTest {

    // -- Set up --
    @TempDir
    Path tempDir;

    private BarSeries testSeries;
    private FakeBroker fakeBroker;
    private TestStrategy testStrategy;

    @BeforeEach
    void setUp() {
        // Set up database
        String testDbPath = tempDir.resolve("test.db").toString();
        Database.connectTestContext("jdbc:sqlite:" + testDbPath);

        // Seed data
        testStrategy = new TestStrategy();
        testSeries = TestSeries.flat(50, 1.1220, 0.006);
        fakeBroker = new FakeBroker(TestSeries.nextBar(testSeries, 1.1240, 0.003));
    }

    // -- Run() Tests --

    // Enter Trade Scenario Tests
    @Test
    void runEnterTradeSuccessTest() {
        // Set the strategy to enter on next run and add the fill details for the next trade
        testStrategy.setEnterNext(true);
        fakeBroker.withMarketOrderFilledTradeDetails("00001", new BigDecimal("1.1415"), Instant.now(), new BigDecimal("10"));

        // Confirm there are no open trades
        assertNull(Database.getOpenTradeId(fakeBroker.getBrokerName(), "EUR_USD"));
        assertEquals(0, fakeBroker.placedMarketOrders.size());

        // Run the engine
        TradingEngine engine = new TradingEngine(fakeBroker, testStrategy, "EUR_USD", testSeries);
        engine.run();

        // Confirm that the trade was placed with the broker and has been logged to the database
        assertEquals(1, fakeBroker.placedMarketOrders.size());
        assertEquals("00001", fakeBroker.placedMarketOrders.getFirst().getTradeId());
        assertEquals("00001", Database.getOpenTradeId(fakeBroker.getBrokerName(), "EUR_USD"));
    }

    @Test
    void runEnterTradeWithOpenTradeTest() {
        // Set the strategy to enter on next run and add the fill details for the next trade
        testStrategy.setEnterNext(true);
        fakeBroker.withMarketOrderFilledTradeDetails("00002", new BigDecimal("1.1215"), Instant.now(), new BigDecimal("10"));

        // Insert an open trade into the database
        Database.insertOpenTrade(fakeBroker.getBrokerName(), "00001", "EUR_USD", 1.1411,
                Instant.now().minus(Duration.ofDays(1)).toString(), 15);

        // Confirm that no trades have been placed in the current session
        assertEquals(0, fakeBroker.placedMarketOrders.size());

        // Run the engine
        TradingEngine engine = new TradingEngine(fakeBroker, testStrategy, "EUR_USD", testSeries);
        engine.run();

        // Confirm that the trade wasn't logged to the database and that the original trade is still open
        assertEquals(0, fakeBroker.placedMarketOrders.size());
        assertEquals("00001", Database.getOpenTradeId(fakeBroker.getBrokerName(), "EUR_USD"));
    }

    // Close Trade Scenario Tests
    @Test
    void runClosePositionSuccessTest() {
        // Open a position
        testStrategy.setEnterNext(true);
        fakeBroker.withMarketOrderFilledTradeDetails("00001", new BigDecimal("1.1215"), Instant.now(), new BigDecimal("10"));
        TradingEngine engine = new TradingEngine(fakeBroker, testStrategy, "EUR_USD", testSeries);
        engine.run();

        // Confirm that the position was opened
        assertEquals("00001", Database.getOpenTradeId(fakeBroker.getBrokerName(), "EUR_USD"));

        // Add the next bar, set to exit next, and run the engine again
        fakeBroker.withNextBar(TestSeries.nextBar(testSeries, 1.1291, 0.01));
        fakeBroker.withClosePositionFilledTradeDetails("00001", new BigDecimal("1.1285"), Instant.now(), new BigDecimal("10"));
        testStrategy.setEnterNext(false);
        testStrategy.setExitNext(true);
        engine.run();

        // Confirm that the position was closed
        assertNull(Database.getOpenTradeId(fakeBroker.getBrokerName(), "EUR_USD"));
        assertEquals(1, fakeBroker.closedPositions.size());
        assertEquals("00001", fakeBroker.closedPositions.getFirst().getTradeId());
    }

    @Test
    void runClosePositionNoOpenTradeTest() {
        // Set up close position
        fakeBroker.withNextBar(TestSeries.nextBar(testSeries, 1.1291, 0.01));
        fakeBroker.withClosePositionFilledTradeDetails("00001", new BigDecimal("1.1285"), Instant.now(), new BigDecimal("10"));
        testStrategy.setExitNext(true);
        TradingEngine engine = new TradingEngine(fakeBroker, testStrategy, "EUR_USD", testSeries);
        engine.run();

        // Confirm that no positions were closed
        assertEquals(0, fakeBroker.closedPositions.size());
    }

    // No Trade Scenario Tests

    @Test
    void runUnderTenAccountBalanceTest() {
        // Confirm database wasn't updated with trade, and it wasn't placed with the broker
        // Next bar is configured in SetUp with close price of 1.1240
        fakeBroker.withBalance(new BigDecimal("9.99"));

        // Verify the current bar
        assertEquals(1.1220, testSeries.getLastBar().getClosePrice().doubleValue());

        TradingEngine engine = new TradingEngine(fakeBroker, testStrategy, "EUR_USD", testSeries);
        engine.run();

        // Confirm the method returned before the new bar was added
        assertEquals(1.1220, testSeries.getLastBar().getClosePrice().doubleValue());
    }

    @Test
    void runTenAccountBalanceTest() {
        // Confirm that the engine proceeds with pulling the latest bar if balance >= 10
        fakeBroker.withBalance(new BigDecimal("10"));

        // Verify the current bar
        assertEquals(1.1220, testSeries.getLastBar().getClosePrice().doubleValue());

        TradingEngine engine = new TradingEngine(fakeBroker, testStrategy, "EUR_USD", testSeries);
        engine.run();

        // Confirm the new bar was added
        assertEquals(1.1240, testSeries.getLastBar().getClosePrice().doubleValue());
    }

    // -- GetLatestBar() tests --

    @Test
    void updateSeriesWithLatestBarNewTest() {
        // Verify the current bar
        assertEquals(1.1220, testSeries.getLastBar().getClosePrice().doubleValue());

        TradingEngine engine = new TradingEngine(fakeBroker, testStrategy, "EUR_USD", testSeries);
        engine.run();

        // Verify new bar was added
        assertEquals(1.1240, testSeries.getLastBar().getClosePrice().doubleValue());
    }

    @Test
    void updateSeriesWithLatestBarSameTest() {
        // Verify the current bar
        assertEquals(1.1220, testSeries.getLastBar().getClosePrice().doubleValue());

        TradingEngine engine = new TradingEngine(fakeBroker, testStrategy, "EUR_USD", testSeries);
        engine.run();

        // Verify new bar was added
        assertEquals(1.1240, testSeries.getLastBar().getClosePrice().doubleValue());
    }

    // -- enterTrade() tests --

    @Test
    void enterTradeZeroPositionSizeTest() {

    }

    @Test
    void enterTradeNoFillTest() {

    }

    @Test
    void enterTradeSuccessTest() {

    }


    // -- closePosition() tests --

    @Test
    void closePositionNoFillTest() {

    }

    @Test
    void closePositionSuccessTest() {

    }

}
