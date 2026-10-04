package bot.config;

import com.oanda.v20.account.AccountID;
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

    public static HashSet<String> getForexInstrumentNames() {
        HashSet<String> forexInstrumentNames = new HashSet<>();
        // Major Pairs
        forexInstrumentNames.add("EUR_USD");
        forexInstrumentNames.add("GBP_USD");
        forexInstrumentNames.add("USD_JPY");
        forexInstrumentNames.add("USD_CHF");
        forexInstrumentNames.add("USD_CAD");
        forexInstrumentNames.add("AUD_USD");
        forexInstrumentNames.add("NZD_USD");

        // Crosses
        forexInstrumentNames.add("EUR_GBP");
        forexInstrumentNames.add("EUR_JPY");
        forexInstrumentNames.add("EUR_CAD");
        forexInstrumentNames.add("EUR_AUD");
        forexInstrumentNames.add("EUR_NZD");

        forexInstrumentNames.add("GBP_JPY");
        forexInstrumentNames.add("GBP_CHF");
        forexInstrumentNames.add("GBP_CAD");
        forexInstrumentNames.add("GBP_AUD");
        forexInstrumentNames.add("GBP_NZD");

        forexInstrumentNames.add("AUD_JPY");
        forexInstrumentNames.add("AUD_CHF");
        forexInstrumentNames.add("AUD_CAD");
        forexInstrumentNames.add("AUD_NZD");

        forexInstrumentNames.add("NZD_JPY");
        forexInstrumentNames.add("NZD_CHF");
        forexInstrumentNames.add("NZD_CAD");

        forexInstrumentNames.add("CAD_JPY");
        forexInstrumentNames.add("CAD_CHF");

        forexInstrumentNames.add("CHF_JPY");

        // forexInstrumentNames.add("EUR_CHF"); // This pair is optional, some people recommend against trading it

        return forexInstrumentNames;
    }
}
