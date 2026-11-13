package bot.data;

import bot.broker.TradeCloseReason;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.crypto.Data;
import java.nio.file.Path;
import java.util.List;

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
        assertThrows(DatabaseException.class, () -> Database.insertOpenTrade("Oanda", "00002", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1));
    }

    // Close an open trade
    @Test
    void closeOpenTradeTest() {
        Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1);
        assertTrue(Database.closeOpenTrade("00001", "Oanda", 1.16015, "2026-09-18 10:32:28", TradeCloseReason.STRATEGY_EXIT));
    }

    // Attempt to close a trade that is already closed
    @Test
    void closeOpenTradeAlreadyClosedTest() {
        Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1);
        Database.closeOpenTrade("00001", "Oanda", 1.16015, "2026-09-18 10:32:28", TradeCloseReason.STRATEGY_EXIT);
        assertFalse(Database.closeOpenTrade("00001", "Oanda", 1.99999, "2026-09-18 11:11:11", TradeCloseReason.STRATEGY_EXIT));
    }

    // Attempt to close a trade that doesn't exist
    @Test
    void closeOpenTradeNotExistTest() {
        assertFalse(Database.closeOpenTrade("00001", "Oanda", 1.99999, "2026-09-18 11:11:11", TradeCloseReason.STRATEGY_EXIT));
    }

    // Attempt to close a trade with the same id, but a different broker
    @Test
    void closeOpenTradeSameIdDifferentBrokerTest() {
        // Create two trades with different brokers, but the same id
        Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 5);
        Database.insertOpenTrade("Alpaca", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 5);

        // Close just the Oanda trade
        assertTrue(Database.closeOpenTrade("00001", "Oanda", 1.16015, "2026-09-18 10:32:28", TradeCloseReason.STRATEGY_EXIT));

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

    // getOpenTradeIds tests
    @Test
    void getOpenTradeIdsTest() {
        // Open 3 trades
        Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45", 1);
        Database.insertOpenTrade("Oanda", "00002", "GBP_USD", 1.3237, "2026-11-13 10:30:45", 1);
        Database.insertOpenTrade("Oanda", "00003", "USD_CAD", 1.4249, "2026-11-13 10:20:10", 1);
        // Close 1 trade
        Database.closeOpenTrade("00001", "Oanda", 1.16015, "2026-09-18 10:32:28", TradeCloseReason.STRATEGY_EXIT);
        // Verify the open trades
        List<String> openTradeIds = Database.getOpenTradeIds("Oanda");
        assertEquals(2, openTradeIds.size());
        assertEquals("00002", openTradeIds.getFirst());
        assertEquals("00003", openTradeIds.getLast());
    }

    @Test
    void getOpenTradeIdsNoOpenTradesTest() {
        List<String> openTradeIds = Database.getOpenTradeIds("Oanda");
        assertEquals(0, openTradeIds.size());
    }

    // Date Format: yyyy-MM-dd HH:mm:ss
}
