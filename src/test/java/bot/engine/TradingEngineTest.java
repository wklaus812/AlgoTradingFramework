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

    // -- Set up test database --

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        String testDbPath = tempDir.resolve("test.db").toString();
        Database.connectTestContext("jdbc:sqlite:" + testDbPath);
    }

    // -- Run() Tests --

    @Test
    void exampleTest() {
        TestSeries.flat(60, 1.10, 0.0020);
    }

    // Enter Trade Scenario Tests
    @Test
    void runEnterTradeNoOpenTradeTest() {
        // Create the strategy and have it enter on next run
        TestStrategy testStrategy = new TestStrategy();
        testStrategy.setEnterNext(true);

        // Create the test series, fake broker, and set the fill details for the next trade
        BarSeries testSeries = TestSeries.flat(50, 1.1220, 0.01);
        FakeBroker fakeBroker = new FakeBroker(TestSeries.nextBar(testSeries, 1.1420, 0.3));
        fakeBroker.withMarketOrderFilledTradeDetails("00001", new BigDecimal("1.1415"), Instant.now(), new BigDecimal("10"));

        // Confirm there are no open trades
        assertNull(Database.getOpenTradeId(fakeBroker.getBrokerName(), "EUR_USD"));

        // Run the engine
        TradingEngine engine = new TradingEngine(
                fakeBroker,
                testStrategy,
                "EUR_USD",
                testSeries
        );
        engine.run();

        // Confirm that the trade has been logged to the database
        assertEquals("00001", Database.getOpenTradeId(fakeBroker.getBrokerName(), "EUR_USD"));
    }

    @Test
    void runEnterTradeWithOpenTrade() {
        // Create the strategy and have it enter on next run
        TestStrategy testStrategy = new TestStrategy();
        testStrategy.setEnterNext(true);

        // Create the test series, fake broker, and set the fill details for the next trade
        BarSeries testSeries = TestSeries.flat(50, 1.1220, 0.01);
        FakeBroker fakeBroker = new FakeBroker(TestSeries.nextBar(testSeries, 1.1420, 0.3));
        fakeBroker.withMarketOrderFilledTradeDetails("00002", new BigDecimal("1.1415"), Instant.now(), new BigDecimal("10"));

        // Insert an open trade into the database
        Database.insertOpenTrade(fakeBroker.getBrokerName(), "00001", "EUR_USD", 1.1411,
                Instant.now().minus(Duration.ofDays(1)).toString(), 15);

        // Run the engine
        TradingEngine engine = new TradingEngine(
                fakeBroker,
                testStrategy,
                "EUR_USD",
                testSeries
        );
        engine.run();

        // Confirm that the trade wasn't logged to the database and that the original trade is still open
        assertEquals("00001", Database.getOpenTradeId(fakeBroker.getBrokerName(), "EUR_USD"));
    }

    // Close Trade Scenario Tests
        // test to confirm open trade
    // No Trade Scenario Tests

    @Test
    void runUnderTenAccountBalanceTest() {
        // Confirm database wasn't updated with trade (if account bal > 10, this scenario should trade)
        TestStrategy testStrategy = new TestStrategy();
        BarSeries testSeries = TestSeries.flat(50, 1.1220, 0.01);
        FakeBroker fakeBroker = new FakeBroker(TestSeries.nextBar(testSeries, 1.1420, 0.3));
        fakeBroker.withBalance(new BigDecimal("9.99"));
        // Verify the current bar
        assertEquals(1.1220, testSeries.getLastBar().getClosePrice().doubleValue());

        TradingEngine engine = new TradingEngine(
                fakeBroker,
                testStrategy,
                "EUR_USD",
                testSeries
        );
        engine.run();

        // Confirm the method returned before the new bar was added
        assertEquals(1.1220, testSeries.getLastBar().getClosePrice().doubleValue());
    }

    @Test
    void runTenAccountBalanceTest() {
        TestStrategy testStrategy = new TestStrategy();
        BarSeries testSeries = TestSeries.flat(50, 1.1220, 0.01);
        FakeBroker fakeBroker = new FakeBroker(TestSeries.nextBar(testSeries, 1.1420, 0.3));
        fakeBroker.withBalance(new BigDecimal("10"));
        // Verify the current bar
        assertEquals(1.1220, testSeries.getLastBar().getClosePrice().doubleValue());

        TradingEngine engine = new TradingEngine(
                fakeBroker,
                testStrategy,
                "EUR_USD",
                testSeries
        );
        engine.run();

        // Confirm the new bar was added
        assertEquals(1.1420, testSeries.getLastBar().getClosePrice().doubleValue());
    }

    // -- GetLatestBar() tests --

    @Test
    void updateSeriesWithLatestBarNewTest() {
        TestStrategy testStrategy = new TestStrategy();
        BarSeries testSeries = TestSeries.flat(50, 1.1220, 0.01);
        FakeBroker fakeBroker = new FakeBroker(TestSeries.nextBar(testSeries, 1.1420, 0.3));
        // Verify the current bar
        assertEquals(1.1220, testSeries.getLastBar().getClosePrice().doubleValue());

        TradingEngine engine = new TradingEngine(
                fakeBroker,
                testStrategy,
                "EUR_USD",
                testSeries
        );
        engine.run();

        // Verify new bar was added
        assertEquals(1.1420, testSeries.getLastBar().getClosePrice().doubleValue());
    }

    @Test
    void updateSeriesWithLatestBarSameTest() {
        TestStrategy testStrategy = new TestStrategy();
        BarSeries testSeries = TestSeries.flat(50, 1.1220, 0.01);
        FakeBroker fakeBroker = new FakeBroker(TestSeries.sameDayBar(testSeries, 1.1420, 0.3));
        // Verify the current bar
        assertEquals(1.1220, testSeries.getLastBar().getClosePrice().doubleValue());

        TradingEngine engine = new TradingEngine(
                fakeBroker,
                testStrategy,
                "EUR_USD",
                testSeries
        );
        engine.run();

        // Verify new bar was added
        assertEquals(1.1420, testSeries.getLastBar().getClosePrice().doubleValue());
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
