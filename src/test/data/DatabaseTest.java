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
        Assertions.assertTrue(Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45"));
    }

    // Ensure there can be multiple open trades with different instruments using the same broker
    @Test
    void insertOpenTradeTwoSameBrokerDifferentInstrumentTest() {
        Assertions.assertTrue(Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45"));
        Assertions.assertTrue(Database.insertOpenTrade("Oanda", "00002", "GBP_USD", 1.35264, "2026-09-12 10:30:45"));
    }

    // Ensure there can be multiple open trades with the same instrument, but a different broker
    @Test
    void insertOpenTradeTwoDifferentBrokerSameInstrumentTest() {
        Assertions.assertTrue(Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45"));
        Assertions.assertTrue(Database.insertOpenTrade("Alpaca", "00002", "EUR_USD", 1.15993, "2026-09-12 10:30:45"));
    }

    // Ensure there can't be multiple open trades with the same instrument/broker
    @Test
    void insertOpenTradeTwoSameBrokerAndInstrumentTest() {
        Assertions.assertTrue(Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45"));
        Assertions.assertFalse(Database.insertOpenTrade("Oanda", "00002", "EUR_USD", 1.15993, "2026-09-12 10:30:45"));
        Database.printAllRows();
    }

    @Test
    void isOpenTradeFalseTest() {
        Assertions.assertFalse(Database.isOpenTrade("Oanda", "EUR_USD"));
    }

    @Test
    void isOpenTradeTrueTest() {
        Assertions.assertTrue(Database.insertOpenTrade("Oanda", "00001", "EUR_USD", 1.15993, "2026-09-12 10:30:45"));
        Assertions.assertTrue(Database.isOpenTrade("Oanda", "EUR_USD"));
    }

    // Date Format: yyyy-MM-dd HH:mm:ss
}
