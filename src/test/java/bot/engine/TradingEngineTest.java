package bot.engine;

import bot.data.Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import bot.testutils.TestSeries;

import java.nio.file.Path;

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
        // test to confirm no open trade
    // Close Trade Scenario Tests
        // test to confirm open trade
    // No Trade Scenario Tests

    @Test
    void runUnderTenAccountBalanceTest() {
        // Confirm database wasn't updated with trade (if account bal > 10, this scenario should trade)
    }

    @Test
    void runTenAccountBalanceTest() {
        // Confirm database was updated with trade
    }

    // -- GetLatestBar() tests --

    @Test
    void getLatestBarNewBarTest() {

    }

    @Test
    void getLatestBarSameBarTest() {

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
