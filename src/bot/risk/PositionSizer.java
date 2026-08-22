package bot.risk;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PositionSizer {

    /**
     * Calculate units to trade (in base currency units) for OANDA given:
     *  - accountBalance: account balance in accountCurrency
     *  - riskPercent: e.g. 0.02 for 2%
     *  - barSeries: Ta4j BarSeries with recent bars
     *  - atrPeriod: e.g. 14
     *  - atrMultiplier: e.g. 2.0
     *  - pair: e.g. "EUR_USD" (underscore or slash as you store)
     *  - currentPrice: current quote price (e.g. 1.12034)
     *  - accountCurrency: e.g. "USD"
     *
     * Returns: units (long) suitable for OANDA (minimum 1 unit).
     */
    public static long calculateUnits(BigDecimal accountBalance,
                                      BigDecimal riskPercent,
                                      BarSeries barSeries,
                                      int atrPeriod,
                                      BigDecimal atrMultiplier,
                                      String pair,
                                      String accountCurrency,
                                      BigDecimal conversionRate) {
//        System.out.println("=========================================== Start Calculate Units");
//        System.out.println(pair);

        // 1) compute ATR using Ta4j
        ClosePriceIndicator close = new ClosePriceIndicator(barSeries);
        ATRIndicator atrInd = new ATRIndicator(barSeries, atrPeriod);
        int endIndex = barSeries.getEndIndex();
        Num atrNum = atrInd.getValue(endIndex);
        BigDecimal atr = BigDecimal.valueOf(atrNum.doubleValue()); // convert from Num to BigDecimal

        // 2) stop distance in price units
        BigDecimal stopDistance = atr.multiply(atrMultiplier).setScale(10, RoundingMode.HALF_UP);
        if (stopDistance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("ATR produced non-positive stop distance");
        }
//        System.out.println("Stop Dist: " + stopDistance);

        // 3) determine pip size for the pair (common rule)
        BigDecimal pipSize = pipSizeForPair(pair); // 0.0001 (most) or 0.01 for JPY pairs
//        System.out.println("Pip Size: " + pipSize);

        // convert stopDistance to pips
        BigDecimal stopPips = stopDistance.divide(pipSize, 8, RoundingMode.HALF_UP);
//        System.out.println("Stop pips:" + stopPips);

        // 4) pip value per 1 unit in account currency.
        // If the quote currency equals account currency: pipValuePerUnit = pipSize
        // For pairs where quoteCurrency != accountCurrency: pipValuePerUnit = pipSize * conversionRate (quote->account)
        String[] parts = pair.replace("/", "_").split("[_]");
        String base = parts[0];
        String quote = parts[1];

        BigDecimal pipValuePerUnit;
        if (quote.equalsIgnoreCase(accountCurrency)) {
            // pip value per *1* base-unit (because 1 unit * pip = pip in account currency)
            pipValuePerUnit = pipSize;
        } else {
            // need conversion rate from quote -> account currency
            // Example: trading EUR/GBP with a USD account: quote = GBP, accountCurrency = USD
            // pipValuePerUnit = pipSize * (rate of quote/account)
//            System.out.println("Conversion rate: " + conversionRate);
            pipValuePerUnit = pipSize.multiply(conversionRate);
        }
//        System.out.println("PipValPerUnit: " + pipValuePerUnit);

        // 5) risk amount in account currency
        BigDecimal riskAmount = accountBalance.multiply(riskPercent);
//        System.out.println("Risk Amount: " + riskAmount);

        // 6) units = riskAmount / (stopPips * pipValuePerUnit)
        BigDecimal denom = stopPips.multiply(pipValuePerUnit);
//        System.out.println("stop pips * pipvalperunit: " + denom);
        if (denom.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Denominator non-positive: check pipValue/stopPips");
        }
        BigDecimal rawUnits = riskAmount.divide(denom, 0, RoundingMode.DOWN); // round down to be safe

        long units = rawUnits.longValue();

        // 7) enforce OANDA min trade size (1 unit for FX)
        if (units < 1) units = 1L;

        // Optional: clamp units to available margin / max-risk limits (implement separately)
//        System.out.println("Units: " + units);
//        System.out.println("========================================= End Calculate Units");
        return units;
    }

    private static BigDecimal pipSizeForPair(String pair) {
        // very simple rule: if pair contains JPY, pip is 0.01 else 0.0001
        if (pair.toUpperCase().contains("JPY")) {
            return new BigDecimal("0.01");
        } else {
            return new BigDecimal("0.0001");
        }
    }
}
