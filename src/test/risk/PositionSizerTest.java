package test.risk;

import main.risk.PositionSizer;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PositionSizerTest {

    // Helper method to simplify calculateUnits calls
    private static long units(String balance, String riskPercent, String stopDistance, String pair, String accountCurrency, String conversionRate) {
        return PositionSizer.calculateUnits(new BigDecimal(balance), new BigDecimal(riskPercent), new BigDecimal(stopDistance), pair, accountCurrency, new BigDecimal(conversionRate));
    }

    // Account Balance Tests
    @Test
    void calculateUnitsBalanceLessThanTenTest() {
        assertEquals(0, units("9.99", "0.01", "0.0001", "EUR_USD", "USD", "1.12"));
    }

    @Test
    void calculateUnitsZeroBalanceTest() {
        assertEquals(0, units("0.00", "0.01", "0.0001", "EUR_USD", "USD", "1.12"));
    }

    @Test
    void calculateUnitsNegativeBalanceTest() {
        assertEquals(0, units("-5.00", "0.01", "0.0001", "EUR_USD", "USD", "1.12"));
    }

    @Test
    void calculateUnitsBalanceEqualTenTest() {
        assertEquals(1000, units("10", "0.01", "0.0001", "EUR_USD", "USD", "1.12"));
    }

    // Pair Tests
    // Balance = 10,000; Risk Percent = 1%; Risk Amount = 100, Stop Distance = 150 pips

    // USD Quoted Pair
    // Position size = 100 / (150 * 0.0001) = 6666 (rounded)
    @Test
    void calculateUnitsUsdQuotedPairTest() {
        assertEquals(6666, units("10000", "0.01", "0.015", "EUR_USD", "USD", "1.12"));
    }

    // JPY Quoted Pair
    // Position size = 100 / (0.01 * 0.0067 * 150) = 9950 (rounded)
    @Test
    void calculateUnitsJpyQuotedPairTest() {
        assertEquals(9950, units("10000", "0.01", "1.5", "USD_JPY", "USD", "0.0067"));
    }

    // USD Based non-JPY Pair
    // Position size = 100 / (0.0001 * 1.25 * 150) = 5333 (rounded)
    @Test
    void calculateUnitsUsdBasedNonJpyPairTest() {
        assertEquals(5333, units("10000", "0.01", "0.015", "USD_CHF", "USD", "1.25"));
    }

    // Cross pair
    // Position size = 100 / (0.0001 * 1.35 * 150) = 4938 (rounded)
    @Test
    void calculateUnitsCrossPairTest() {
        assertEquals(4938, units("10000", "0.01", "0.015", "EUR_GBP", "USD", "1.35"));
    }

    // Non-USD account currency
    @Test
    void calculateUnitsNonUsdAccountCurrencyTest() {
        assertEquals(7246, units("10000", "0.01", "0.015", "EUR_USD", "EUR", "0.92"));
    }

    @Test
    void calculateUnitsNonUsdAccountCurrencyMatchingQuoteCurrencyTest() {
        assertEquals(6666, units("10000", "0.01", "0.015", "EUR_GBP", "GBP", "999"));
    }

    // Edge Cases
    @Test
    void calculateUnitsResultUnitsLessThanOneTest() {
        assertEquals(0, units("10", "0.01", "0.5", "EUR_USD", "USD", "1"));
    }

    @Test
    void calculateUnitsZeroRiskPercentTest() {
        assertEquals(0, units("10000", "0", "0.015", "EUR_USD", "USD", "1"));
    }

    @Test
    void calculateUnitsNegativeRiskPercentTest() {
        assertEquals(0, units("10000", "-0.01", "0.015", "EUR_USD", "USD", "1"));
    }

    @Test
    void calculateUnitsZeroStopDistanceTest() {
        assertThrows(IllegalStateException.class, () -> units("10000", "0.01", "0", "EUR_USD", "USD", "1"));
    }

    @Test
    void calculateUnitsNegativeStopDistanceTest() {
        assertThrows(IllegalStateException.class, () -> units("10000", "0.01", "-0.015", "EUR_USD", "USD", "1"));
    }

}
