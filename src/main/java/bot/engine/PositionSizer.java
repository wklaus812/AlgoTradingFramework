package bot.engine;

import bot.Config;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PositionSizer {

    /**
     * Calculate units to trade (in the provided account currency) for forex pairs:
     *  - accountBalance: account balance in accountCurrency
     *  - riskPercent: e.g. 0.02 for 2%
     *  - StopLossDistance: Represented in quote currency
     *  - pair: e.g. "EUR_USD" (underscore or slash as you store)
     *  - currentPrice: current quote price (e.g. 1.12034)
     *  - accountCurrency: e.g. "USD"
     */
    public static long calculateUnits(BigDecimal accountBalance,
                                      BigDecimal riskPercent,
                                      BigDecimal stopLossDistance,
                                      String pair,
                                      String accountCurrency,
                                      BigDecimal conversionRate) {
        // If account balance is < Min Balance to Trade ($10), don't trade
        if (accountBalance.doubleValue() < Config.MIN_BALANCE_TO_TRADE) {
            return 0;
        }

        // Determine pip size for the pair
        BigDecimal pipSize = pipSizeForPair(pair); // 0.0001 (most) or 0.01 for JPY pairs

        // Convert stopDistance to pips
        BigDecimal stopPips = stopLossDistance.divide(pipSize, 8, RoundingMode.HALF_UP);

        // Pip value per 1 unit in account currency.
        // If the quote currency equals account currency: pipValuePerUnit = pipSize
        // For pairs where quoteCurrency != accountCurrency: pipValuePerUnit = pipSize * conversionRate (quote->account)
        String[] parts = pair.replace("/", "_").split("[_]");
        String quote = parts[1];

        BigDecimal pipValuePerUnit;
        if (quote.equalsIgnoreCase(accountCurrency)) {
            // Already in account currency (USD)
            pipValuePerUnit = pipSize;
        } else {
            // Get conversion rate from quote to USD
            // Ex: trading EUR/GBP with a USD account: quote = GBP, accountCurrency = USD
            // pipValuePerUnit = pipSize * (rate of quote/account)
            pipValuePerUnit = pipSize.multiply(conversionRate);
        }

        // Risk amount in account currency (USD)
        BigDecimal riskAmount = accountBalance.multiply(riskPercent);

        // Units = riskAmount / (stopPips * pipValuePerUnit)
        BigDecimal denom = stopPips.multiply(pipValuePerUnit);

        if (denom.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Denominator non-positive: check pipValue/stopPips");
        }
        BigDecimal rawUnits = riskAmount.divide(denom, 0, RoundingMode.DOWN); // Round down to be safe

        long units = rawUnits.longValue();

        // If units < 1, don't trade
        if (units < 1) return 0;

        return units;
    }

    private static BigDecimal pipSizeForPair(String pair) {
        // If pair contains JPY, pip is 0.01 else 0.0001
        if (pair.toUpperCase().contains("JPY")) {
            return new BigDecimal("0.01");
        } else {
            return new BigDecimal("0.0001");
        }
    }
}
