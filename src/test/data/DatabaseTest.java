package test.data;

import bot.data.Database;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class DatabaseTest {

    @BeforeEach
    void setUp() {
        Database.connect();
    }

    @Test
    void checkForOpenTradeTest() {
        Assertions.assertFalse(Database.checkForOpenTrade("Oanda", "001"));
    }
}
