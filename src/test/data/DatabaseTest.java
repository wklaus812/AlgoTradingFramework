package test.data;

import bot.data.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

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
        assertTrue(Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1));
    }

    // Ensure there can be multiple open trades with different instruments using the same broker
    @Test
    void insertOpenTradeTwoSameBrokerDifferentInstrumentTest() {
        assertTrue(Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1));
        assertTrue(Database.insertOpenTrade("Oanda", "00002", "GBP_USD", 1.35264, "2026-09-12 10:30:45", 1));
    }

    // Ensure there can be multiple open trades with the same instrument, but a different broker
    @Test
    void insertOpenTradeTwoDifferentBrokerSameInstrumentTest() {
        assertTrue(Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1));
        assertTrue(Database.insertOpenTrade("Alpaca", "00002", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1));
    }

    // Ensure there can't be multiple open trades with the same instrument/broker
    @Test
    void insertOpenTradeTwoSameBrokerAndInstrumentTest() {
        assertTrue(Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1));
        assertFalse(Database.insertOpenTrade("Oanda", "00002", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1));
    }

    // Close an open trade
    @Test
    void closeOpenTradeTest() {
        Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1);
        assertTrue(Database.closeOpenTrade("00001", "Oanda", 1.16015, "2026-09-18 10:32:28"));
    }

    // Attempt to close a trade that is already closed
    @Test
    void closeOpenTradeAlreadyClosedTest() {
        Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1);
        Database.closeOpenTrade("00001", "Oanda", 1.16015, "2026-09-18 10:32:28");
        assertFalse(Database.closeOpenTrade("00001", "Oanda", 1.99999, "2026-09-18 11:11:11"));
    }

    // Attempt to close a trade that doesn't exist
    @Test
    void closeOpenTradeNotExistTest() {
        assertFalse(Database.closeOpenTrade("00001", "Oanda", 1.99999, "2026-09-18 11:11:11"));
    }

    // Attempt to close a trade with the same id, but a different broker
    @Test
    void closeOpenTradeSameIdDifferentBrokerTest() {
        // Create two trades with different brokers, but the same id
        Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 5);
        Database.insertOpenTrade("Alpaca", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 5);

        // Close just the Oanda trade
        assertTrue(Database.closeOpenTrade("00001", "Oanda", 1.16015, "2026-09-18 10:32:28"));

        // Verify that the Alpaca trade is still open
        assertNotNull(Database.getOpenTradeId("Alpaca", "EUR_USD"));
    }

    @Test
    void getOpenTradeIdFalseTest() {
        assertNull(Database.getOpenTradeId("Oanda", "EUR_USD"));
    }

    @Test
    void getOpenTradeIdFalseForDifferentInstrumentTest() {
        Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1);
        assertNull(Database.getOpenTradeId("Oanda", "GBP_USD"));
    }

    @Test
    void getOpenTradeIdTrueTest() {
        assertTrue(Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1));
        assertNotNull(Database.getOpenTradeId("Oanda", "EUR_USD"));
    }

    // Date Format: yyyy-MM-dd HH:mm:ss
}
