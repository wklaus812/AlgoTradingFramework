package bot.data;

import com.oanda.v20.Context;
import com.oanda.v20.ContextBuilder;
import com.oanda.v20.pricing.ClientPrice;
import com.oanda.v20.pricing.PricingGetRequest;
import com.oanda.v20.pricing.PricingGetResponse;
import com.oanda.v20.primitives.InstrumentName;
import org.ta4j.core.BarSeries;

import bot.config.Config;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;


// Deprecated
public class CurrencyConverter {

    public static HashMap<InstrumentName, Double> getClosePriceInUsdByInstrumentName(HashMap<InstrumentName, BarSeries> barSeriesByInstrumentName) throws Exception {
        HashMap<InstrumentName, Double> closePriceInUsdByInstrumentName = new HashMap<>();

        for (InstrumentName instrumentName : barSeriesByInstrumentName.keySet()) {
            BarSeries series = barSeriesByInstrumentName.get(instrumentName);
            //Double closePriceInUsd = getClosePriceInUsd(instrumentName, series.getLastBar().getClosePrice().doubleValue(), barSeriesByInstrumentName);
            Double closePriceInUsd = convertCurrency(instrumentName, BigDecimal.valueOf(series.getLastBar().getClosePrice().doubleValue())).doubleValue();
            closePriceInUsdByInstrumentName.put(instrumentName, closePriceInUsd);
            //System.out.println(instrumentName + ": " + convertCurrency(instrumentName, BigDecimal.valueOf(series.getLastBar().getClosePrice().doubleValue())));
        }

        return closePriceInUsdByInstrumentName;
    }

    // Deprecated, didn't work correctly
    private static Double getClosePriceInUsd(InstrumentName instrumentName, Double closePrice, HashMap<InstrumentName, BarSeries> barSeriesByInstrumentName) {
        // Check if base currency is usd
        // If not, see if quote currency is
        // If not, get usd pair with base currency
            //

        String baseCurrency = instrumentName.toString().substring(0, 3);
        String quoteCurrency = instrumentName.toString().substring(4, 7);
        if (quoteCurrency.equals("USD")) {
            // Pair already quoted in USD
            return closePrice;
        } else if (baseCurrency.equals("USD")) {
            // Pair Based in USD
            return 1 / closePrice;
        } else {
            // Pair is a cross
            String usdPair = quoteCurrency + "_USD";
            if (barSeriesByInstrumentName.containsKey(new InstrumentName(usdPair))) {
                // New pair quoted in USD
                Double usdPairClosePrice = barSeriesByInstrumentName.get(new InstrumentName(usdPair)).getLastBar().getClosePrice().doubleValue();
                return closePrice * usdPairClosePrice;
            } else {
                // New pair based in USD
                usdPair = "USD_" + quoteCurrency;
                Double usdPairClosePrice = barSeriesByInstrumentName.get(new InstrumentName(usdPair)).getLastBar().getClosePrice().doubleValue();
                return closePrice / usdPairClosePrice;
            }
        }
    }


    private static BigDecimal convertCurrency(InstrumentName instrument, BigDecimal amount) throws Exception {
        Context ctx = new ContextBuilder(Config.URL)
                .setToken(Config.TOKEN)
                .setApplication("java.data.CurrencyConverter")
                .build();

        // If OANDA doesn't have a direct pair, you could add cross logic later
        ArrayList<InstrumentName> instruments = new ArrayList<>();
        instruments.add(instrument);
        PricingGetRequest req = new PricingGetRequest(Config.ACCOUNT_ID, instruments);

        PricingGetResponse resp = ctx.pricing.get(req);
        ClientPrice price = resp.getPrices().getFirst();
        return BigDecimal.valueOf(price.getQuoteHomeConversionFactors().getPositiveUnits().doubleValue());
    }

}
