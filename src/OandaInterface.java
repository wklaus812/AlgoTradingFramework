import com.oanda.v20.Context;
import com.oanda.v20.ContextBuilder;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.account.*;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.instrument.InstrumentCandlesResponse;
import com.oanda.v20.order.MarketOrderRequest;
import com.oanda.v20.order.OrderCreateRequest;
import com.oanda.v20.order.OrderCreateResponse;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.trade.TradeCloseRequest;
import com.oanda.v20.trade.TradeCloseResponse;
import com.oanda.v20.trade.TradeSpecifier;
import com.oanda.v20.transaction.OrderFillTransaction;
import com.oanda.v20.transaction.TransactionID;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNum;

import java.time.*;
import java.util.List;

public class OandaInterface {

    public OandaInterface() {}

    public BarSeries getBarSeriesFromOanda(InstrumentName instrumentName) throws ExecuteException, RequestException {
        BarSeries series = new BaseBarSeriesBuilder().withName("oanda_candles").build();

        Context ctx = new ContextBuilder(Config.URL)
                .setToken(Config.TOKEN)
                .setApplication("GetBars")
                .build();

        InstrumentCandlesRequest request = new InstrumentCandlesRequest(instrumentName)
                .setGranularity(CandlestickGranularity.D)
                .setAlignmentTimezone("America/Chicago")
                .setCount(Integer.toUnsignedLong(250));

        try {
            InstrumentCandlesResponse response = ctx.instrument.candles(request);

            for (Candlestick candle : response.getCandles()) {
                ZonedDateTime endTime = getOandaEndTime(candle);

                // Create Bar and add to list
                BaseBar bar = new BaseBar(
                        Duration.ofDays(1),
                        Instant.from(endTime),
                        DecimalNum.valueOf(candle.getMid().getO().doubleValue()),
                        DecimalNum.valueOf(candle.getMid().getH().doubleValue()),
                        DecimalNum.valueOf(candle.getMid().getL().doubleValue()),
                        DecimalNum.valueOf(candle.getMid().getC().doubleValue()),
                        DecimalNum.valueOf(candle.getVolume()),
                        null,
                        0
                );
                series.addBar(bar);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

        return series;
    }

    //
    public BaseBar getLatestBar(InstrumentName instrument) throws ExecuteException, RequestException {
        Context ctx = new ContextBuilder(Config.URL)
                .setToken(Config.TOKEN)
                .setApplication("GetLatestBar")
                .build();

        InstrumentCandlesRequest request = new InstrumentCandlesRequest(instrument)
                .setGranularity(CandlestickGranularity.D)
                .setAlignmentTimezone("America/Chicago")
                .setCount(Integer.toUnsignedLong(1));

        try {
            InstrumentCandlesResponse response = ctx.instrument.candles(request);
            Candlestick latestCandle = response.getCandles().getLast();

            ZonedDateTime endTime = getOandaEndTime(latestCandle);

            // Return bar
            return new BaseBar(
                    Duration.ofDays(1),
                    Instant.from(endTime),
                    DecimalNum.valueOf(latestCandle.getMid().getO().doubleValue()),
                    DecimalNum.valueOf(latestCandle.getMid().getH().doubleValue()),
                    DecimalNum.valueOf(latestCandle.getMid().getL().doubleValue()),
                    DecimalNum.valueOf(latestCandle.getMid().getC().doubleValue()),
                    DecimalNum.valueOf(latestCandle.getVolume()),
                    null,
                    0
            );
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    private ZonedDateTime getOandaEndTime(Candlestick candle) {
        String dateTimeString = candle.getTime().toString();
        // Build LocalDate
        int year = Integer.parseInt(dateTimeString.substring(0,4));
        int month = Integer.parseInt(dateTimeString.substring(5,7));
        int day = Integer.parseInt(dateTimeString.substring(8,10));
        LocalDate localDate = LocalDate.of(year, month, day);
        // Build LocalTime
        int hour = Integer.parseInt(dateTimeString.substring(11,13));
        int minute = Integer.parseInt(dateTimeString.substring(14,16));
        int second = Integer.parseInt(dateTimeString.substring(17,19));
        LocalTime localTime = LocalTime.of(hour,minute,second);
        // Build LocalDateTime
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
        // Build OffsetDateTime
        OffsetDateTime offsetDateTime = OffsetDateTime.of(localDateTime, ZoneOffset.of("-05:00"));
        // Build ZonedDateTime and return
        return ZonedDateTime.of(offsetDateTime.getYear(), offsetDateTime.getMonthValue(),
                offsetDateTime.getDayOfMonth(), offsetDateTime.getHour(), offsetDateTime.getMinute(),
                offsetDateTime.getSecond(), offsetDateTime.getNano(), ZoneId.of("America/Chicago"));
    }

    public TransactionID placeMarketOrder(InstrumentName instrument) {
        Context ctx = new ContextBuilder(Config.URL)
                .setToken(Config.TOKEN)
                .setApplication("PlaceMarketOrder")
                .build();

        AccountID accountId = Config.ACCOUNTID;
        validateAccount(ctx, accountId);

        // Place market order
        TransactionID tradeId;
        try {
            OrderCreateRequest request = new OrderCreateRequest(accountId);

            MarketOrderRequest marketOrderRequest = new MarketOrderRequest();
            marketOrderRequest.setInstrument(instrument);
            marketOrderRequest.setUnits(100);                                      // Determine this using average true range
            request.setOrder(marketOrderRequest);

            OrderCreateResponse response = ctx.order.create(request);
            OrderFillTransaction transaction = response.getOrderFillTransaction();

            tradeId = transaction.getId();
            return tradeId;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void closePosition(InstrumentName instrument, TransactionID tradeId) {
        Context ctx = new ContextBuilder(Config.URL)
                .setToken(Config.TOKEN)
                .setApplication("PlaceMarketOrder")
                .build();

        AccountID accountId = Config.ACCOUNTID;
        validateAccount(ctx, accountId);

        // Place market order
        try {
            TradeCloseResponse response = ctx.trade.close(new TradeCloseRequest(accountId, new TradeSpecifier(tradeId.toString())));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void validateAccount(Context ctx, AccountID accountId) {
        // Ensure account exists
        try {
            // Execute the request and obtain a response object
            AccountListResponse response = ctx.account.list();
            // Retrieve account list from response object
            List<AccountProperties> accountProperties;
            accountProperties = response.getAccounts();
            // Check for the configured account
            boolean hasaccount = false;
            for (AccountProperties account : accountProperties) {
                if (account.getId().equals(accountId))
                    hasaccount = true;
            }
            if (!hasaccount)
                throw new RuntimeException("Account "+accountId+" not found");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Ensure account has non-zero balance
        try {
            // Execute the request and retrieve a response object
            AccountGetResponse response = ctx.account.get(accountId);
            // Retrieve the contents of the result
            Account account;
            account = response.getAccount();
            // Check the balance
            if (account.getBalance().doubleValue() <= 0.0)
                throw new RuntimeException("Account "+accountId+" balance "+account.getBalance()+" <= 0");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
