package bot.config;

import com.oanda.v20.account.AccountID;
import com.oanda.v20.primitives.InstrumentName;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.HashSet;

public class Config {
    private Config() {}
    private static final Dotenv dotenv = Dotenv.load();
    public static final String URL = "https://api-fxpractice.oanda.com";
    public static final String TOKEN = getEnv("OANDA_API_TOKEN");
    public static final AccountID ACCOUNT_ID = new AccountID(getEnv("OANDA_ACCOUNT_ID"));

    private static String getEnv(String name) {
        String value = dotenv.get(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }

        return value;
    }

    public static HashSet<InstrumentName> getForexInstrumentNames() {
        HashSet<InstrumentName> forexInstrumentNames = new HashSet<>();
        // Major Pairs
        forexInstrumentNames.add(new InstrumentName("EUR_USD"));
        forexInstrumentNames.add(new InstrumentName("GBP_USD"));
        forexInstrumentNames.add(new InstrumentName("USD_JPY"));
        forexInstrumentNames.add(new InstrumentName("USD_CHF"));
        forexInstrumentNames.add(new InstrumentName("USD_CAD"));
        forexInstrumentNames.add(new InstrumentName("AUD_USD"));
        forexInstrumentNames.add(new InstrumentName("NZD_USD"));

        // Crosses
        forexInstrumentNames.add(new InstrumentName("EUR_GBP"));
        forexInstrumentNames.add(new InstrumentName("EUR_JPY"));
        forexInstrumentNames.add(new InstrumentName("EUR_CAD"));
        forexInstrumentNames.add(new InstrumentName("EUR_AUD"));
        forexInstrumentNames.add(new InstrumentName("EUR_NZD"));

        forexInstrumentNames.add(new InstrumentName("GBP_JPY"));
        forexInstrumentNames.add(new InstrumentName("GBP_CHF"));
        forexInstrumentNames.add(new InstrumentName("GBP_CAD"));
        forexInstrumentNames.add(new InstrumentName("GBP_AUD"));
        forexInstrumentNames.add(new InstrumentName("GBP_NZD"));

        forexInstrumentNames.add(new InstrumentName("AUD_JPY"));
        forexInstrumentNames.add(new InstrumentName("AUD_CHF"));
        forexInstrumentNames.add(new InstrumentName("AUD_CAD"));
        forexInstrumentNames.add(new InstrumentName("AUD_NZD"));

        forexInstrumentNames.add(new InstrumentName("NZD_JPY"));
        forexInstrumentNames.add(new InstrumentName("NZD_CHF"));
        forexInstrumentNames.add(new InstrumentName("NZD_CAD"));

        forexInstrumentNames.add(new InstrumentName("CAD_JPY"));
        forexInstrumentNames.add(new InstrumentName("CAD_CHF"));

        forexInstrumentNames.add(new InstrumentName("CHF_JPY"));

        // forexInstrumentNames.add(new InstrumentName("EUR_CHF")); // This pair is optional, some people recommend against trading it

        return forexInstrumentNames;
    }
}
