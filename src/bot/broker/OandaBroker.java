package bot.broker;

import com.oanda.v20.Context;
import com.oanda.v20.ContextBuilder;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.account.*;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.instrument.InstrumentCandlesResponse;
import com.oanda.v20.order.*;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.trade.TradeCloseRequest;
import com.oanda.v20.trade.TradeCloseResponse;
import com.oanda.v20.trade.TradeSpecifier;
import com.oanda.v20.transaction.*;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNum;

import bot.config.Config;
import bot.engine.OrderDetails;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public class OandaBroker implements Broker<TransactionID, InstrumentName> {

    public OandaBroker() {}

    public BigDecimal getAccountBalance() {
        // Gets the current balance of the account
        Context ctx = new ContextBuilder(Config.URL).setToken(Config.TOKEN).setApplication("GetAccountDetails").build();
        BigDecimal accountBalance = new BigDecimal("0.0");

        try {
            AccountGetResponse acc = ctx.account.get(Config.ACCOUNT_ID);
            accountBalance = acc.getAccount().getBalance().bigDecimalValue();
        } catch (RequestException | ExecuteException e) {
            throw new BrokerException("Unable to retrieve Account Balance", e);
        }
        return accountBalance;
    }

    public BarSeries getHistoricalBarSeries(InstrumentName instrument) {
        BarSeries series = new BaseBarSeriesBuilder().withName("oanda_candles").build();

        Context ctx = new ContextBuilder(Config.URL)
                .setToken(Config.TOKEN)
                .setApplication("GetBars")
                .build();

        InstrumentCandlesRequest request = new InstrumentCandlesRequest(instrument)
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
                        Instant.from(endTime),               // UPDATE THIS TO GET BEGIN TIME
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
        } catch (RequestException | ExecuteException e) {
            throw new BrokerException("Unable to retrieve historical bar series for " + instrument, e);
        }

        return series;
    }

    public BaseBar getLatestBar(InstrumentName instrument)  {
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
                    Instant.from(endTime),              // UPDATE THIS TO GET BEGIN TIME
                    Instant.from(endTime),
                    DecimalNum.valueOf(latestCandle.getMid().getO().doubleValue()),
                    DecimalNum.valueOf(latestCandle.getMid().getH().doubleValue()),
                    DecimalNum.valueOf(latestCandle.getMid().getL().doubleValue()),
                    DecimalNum.valueOf(latestCandle.getMid().getC().doubleValue()),
                    DecimalNum.valueOf(latestCandle.getVolume()),
                    null,
                    0
            );
        } catch (RequestException | ExecuteException e) {
            throw new BrokerException("Unable to retrieve latest bar for " + instrument, e);
        }
    }

    public TransactionID placeMarketOrder(InstrumentName instrument, int tradeSize, OrderDetails details) {
        Context ctx = new ContextBuilder(Config.URL)
                .setToken(Config.TOKEN)
                .setApplication("PlaceMarketOrder")
                .build();
        AccountID accountId = Config.ACCOUNT_ID;
        validateAccount(ctx, accountId);

        // Build stop loss and take profit details if necessary
        StopLossDetails stopLossDetails = getStopLossDetails(details);
        TrailingStopLossDetails trailingStopLossDetails = getTrailingStopLossDetails(details);
        TakeProfitDetails takeProfitDetails = getTakeProfitDetails(details);

        // Place market order
        TransactionID txnId;
        try {
            OrderCreateRequest request = new OrderCreateRequest(accountId);

            MarketOrderRequest marketOrderRequest = new MarketOrderRequest();
            marketOrderRequest.setInstrument(instrument);
            marketOrderRequest.setUnits(tradeSize);
            // Set stop loss if specified
            if (stopLossDetails != null) {
                marketOrderRequest.setStopLossOnFill(stopLossDetails);
            } else if (trailingStopLossDetails != null) {
                marketOrderRequest.setTrailingStopLossOnFill(trailingStopLossDetails);
            }
            // Set take profit if specified
            if (takeProfitDetails != null) marketOrderRequest.setTakeProfitOnFill(takeProfitDetails);
            request.setOrder(marketOrderRequest);

            OrderCreateResponse response = ctx.order.create(request);
            OrderFillTransaction transaction = response.getOrderFillTransaction();

            txnId = transaction.getId();
            return txnId;

        } catch (RequestException | ExecuteException e) {
            throw new BrokerException("Unable to place market order for " + instrument, e);
        }
    }

    public void closePosition(InstrumentName instrument, TransactionID tradeId) {
        Context ctx = new ContextBuilder(Config.URL)
                .setToken(Config.TOKEN)
                .setApplication("PlaceMarketOrder")
                .build();

        AccountID accountId = Config.ACCOUNT_ID;
        validateAccount(ctx, accountId);

        // Place market order
        try {
            TradeCloseResponse response = ctx.trade.close(new TradeCloseRequest(accountId, new TradeSpecifier(tradeId.toString())));

        } catch (RequestException | ExecuteException e) {
            throw new BrokerException("Unable to close position for " + instrument, e);
        }
    }

    // Helper method used for placing trades. Ensures the account exists and has a non-zero balance.
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
                throw new BrokerException("Account "+accountId+" not found");
        } catch (RequestException | ExecuteException e) {
            throw new BrokerException("Unable to find Oanda Account", e);
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
                throw new BrokerException("Account "+accountId+" balance "+account.getBalance()+" <= 0");
        } catch (RequestException | ExecuteException e) {
            throw new BrokerException("Unable to find Oanda Account", e);
        }
    }

    // Helper method that parses the time returned from Oanda for a given candle/bar
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

    // Builds Oanda stop loss details, based on provided distance
    private StopLossDetails getStopLossDetails(OrderDetails details) {
        // Return null if trailing stop loss is specified
        if (details.getIsTrailingStopLoss()) return null;

        StopLossDetails stopLossDetails = new StopLossDetails();
        stopLossDetails.setDistance(details.getStopLossDistance());
        return stopLossDetails;
    }

    // Builds Oanda trailing stop loss details, based on provided distance
    private TrailingStopLossDetails getTrailingStopLossDetails(OrderDetails details) {
        // Return null if normal stop loss is specified
        if (!details.getIsTrailingStopLoss()) return null;

        TrailingStopLossDetails trailingStopLossDetails = new TrailingStopLossDetails();
        trailingStopLossDetails.setDistance(details.getStopLossDistance());
        return trailingStopLossDetails;
    }

    // Builds Oanda take profit details, based on the provided price
    private TakeProfitDetails getTakeProfitDetails(OrderDetails details) {
        if (details.getTakeProfitPrice() == null) return null;

        TakeProfitDetails takeProfitDetails = new TakeProfitDetails();
        takeProfitDetails.setPrice(details.getTakeProfitPrice());
        return takeProfitDetails;
    }
}
