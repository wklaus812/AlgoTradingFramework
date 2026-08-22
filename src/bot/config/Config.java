package bot.config;

import com.oanda.v20.account.AccountID;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.transaction.TransactionID;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.HashMap;

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

    public static HashMap<InstrumentName, TransactionID> getTransactionIdsByInstrumentName() {
        HashMap<InstrumentName, TransactionID> transactionIdsByInstrumentName = new HashMap<InstrumentName, TransactionID>();
        // Major Pairs
        transactionIdsByInstrumentName.put(new InstrumentName("EUR_USD"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("GBP_USD"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("USD_JPY"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("USD_CHF"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("USD_CAD"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("AUD_USD"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("NZD_USD"), null);

        // Crosses
        transactionIdsByInstrumentName.put(new InstrumentName("EUR_GBP"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("EUR_JPY"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("EUR_CAD"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("EUR_AUD"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("EUR_NZD"), null);

        transactionIdsByInstrumentName.put(new InstrumentName("GBP_JPY"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("GBP_CHF"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("GBP_CAD"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("GBP_AUD"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("GBP_NZD"), null);

        transactionIdsByInstrumentName.put(new InstrumentName("AUD_JPY"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("AUD_CHF"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("AUD_CAD"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("AUD_NZD"), null);

        transactionIdsByInstrumentName.put(new InstrumentName("NZD_JPY"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("NZD_CHF"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("NZD_CAD"), null);

        transactionIdsByInstrumentName.put(new InstrumentName("CAD_JPY"), null);
        transactionIdsByInstrumentName.put(new InstrumentName("CAD_CHF"), null);

        transactionIdsByInstrumentName.put(new InstrumentName("CHF_JPY"), null);

        // transactionIdsByInstrumentName.put(new InstrumentName("EUR_CHF"), null); // This pair is optional, some people recommend against trading it

        return transactionIdsByInstrumentName;
    }
}
