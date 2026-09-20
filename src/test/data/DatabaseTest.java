package test.data;

import bot.data.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

//import static org.junit.jupiter.api.Assertions.*;

public class DatabaseTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        String testDbPath = tempDir.resolve("test.db").toString();
        Database.connectTestContext("jdbc:sqlite:" + testDbPath);
    }

    @Test
    void insertOpenTradeSingleTest() {
        Assertions.assertTrue(Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1));
    }

    // Ensure there can be multiple open trades with different instruments using the same broker
    @Test
    void insertOpenTradeTwoSameBrokerDifferentInstrumentTest() {
        Assertions.assertTrue(Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1));
        Assertions.assertTrue(Database.insertOpenTrade("Oanda", "00002", "GBP_USD", 1.35264, "2026-09-12 10:30:45", 1));
    }

    // Ensure there can be multiple open trades with the same instrument, but a different broker
    @Test
    void insertOpenTradeTwoDifferentBrokerSameInstrumentTest() {
        Assertions.assertTrue(Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1));
        Assertions.assertTrue(Database.insertOpenTrade("Alpaca", "00002", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1));
    }

    // Ensure there can't be multiple open trades with the same instrument/broker
    @Test
    void insertOpenTradeTwoSameBrokerAndInstrumentTest() {
        Assertions.assertTrue(Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1));
        Assertions.assertFalse(Database.insertOpenTrade("Oanda", "00002", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1));
    }

    // Close an open trade
    @Test
    void closeOpenTradeTest() {
        Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1);
        Assertions.assertTrue(Database.closeOpenTrade("00001", "Oanda", 1.16015, "2026-09-18 10:32:28"));
    }

    // Attempt to close a trade that is already closed
    @Test
    void closeOpenTradeAlreadyClosedTest() {
        Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1);
        Database.closeOpenTrade("00001", "Oanda", 1.16015, "2026-09-18 10:32:28");
        Assertions.assertFalse(Database.closeOpenTrade("00001", "Oanda", 1.99999, "2026-09-18 11:11:11"));
    }

    // Attempt to close a trade that doesn't exist
    @Test
    void closeOpenTradeNotExistTest() {
        Assertions.assertFalse(Database.closeOpenTrade("00001", "Oanda", 1.99999, "2026-09-18 11:11:11"));
    }

    // Attempt to close a trade with the same id, but a different broker
    @Test
    void closeOpenTradeSameIdDifferentBrokerTest() {
        // Create two trades with different brokers, but the same id
        Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 5);
        Database.insertOpenTrade("Alpaca", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 5);

        // Close just the Oanda trade
        Assertions.assertTrue(Database.closeOpenTrade("00001", "Oanda", 1.16015, "2026-09-18 10:32:28"));

        // Verify that the Alpaca trade is still open
        Assertions.assertNotNull(Database.getOpenTradeId("Alpaca", "EUR_USD"));
    }

    @Test
    void getOpenTradeIdFalseTest() {
        Assertions.assertNull(Database.getOpenTradeId("Oanda", "EUR_USD"));
    }

    @Test
    void getOpenTradeIdFalseForDifferentInstrumentTest() {
        Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1);
        Assertions.assertNull(Database.getOpenTradeId("Oanda", "GBP_USD"));
    }

    @Test
    void getOpenTradeIdTrueTest() {
        Assertions.assertTrue(Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1));
        Assertions.assertNotNull(Database.getOpenTradeId("Oanda", "EUR_USD"));
    }

    // Date Format: yyyy-MM-dd HH:mm:ss
}
