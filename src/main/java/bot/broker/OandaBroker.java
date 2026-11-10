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
import com.oanda.v20.pricing.ClientPrice;
import com.oanda.v20.pricing.PricingGetRequest;
import com.oanda.v20.pricing.PricingGetResponse;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.trade.*;
import com.oanda.v20.transaction.*;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNum;

import bot.Config;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;

public class OandaBroker implements Broker{

    private final Context ctx;
    private final AccountID accountId;

    public OandaBroker() {
        ctx = new ContextBuilder(Config.URL).setToken(Config.TOKEN).setApplication("OandaBroker").build();
        accountId = new AccountID(Config.ACCOUNT_ID);
    }

    public OandaBroker(Context ctx, AccountID accountId) {
        this.ctx = ctx;
        this.accountId = accountId;
    }

    @Override
    public BigDecimal getAccountBalance() {
        BigDecimal accountBalance;

        try {
            AccountGetResponse acc = ctx.account.get(accountId);
            accountBalance = acc.getAccount().getBalance().bigDecimalValue();
        } catch (RequestException | ExecuteException e) {
            throw new BrokerException("Unable to retrieve Account Balance", e);
        }
        return accountBalance;
    }

    @Override
    public BarSeries getHistoricalBarSeries(String instrument) {
        BarSeries series = new BaseBarSeriesBuilder().withName("oanda_candles").build();

        InstrumentCandlesRequest request = new InstrumentCandlesRequest(new InstrumentName(instrument))
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
                        null,
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

    @Override
    public BaseBar getLatestBar(String instrument)  {
        InstrumentCandlesRequest request = new InstrumentCandlesRequest(new InstrumentName(instrument))
                .setGranularity(CandlestickGranularity.D)
                .setAlignmentTimezone("America/Chicago")
                .setCount(Integer.toUnsignedLong(1));

        try {
            InstrumentCandlesResponse response = ctx.instrument.candles(request);
            Candlestick latestCandle = response.getCandles().getLast();

            ZonedDateTime endTime = getOandaEndTime(latestCandle);

            // Return bar as TA4J Bar
            return new BaseBar(
                    Duration.ofDays(1),
                    null,
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

    // Returns the TradeId as a String
    @Override
    public FilledTradeDetails placeMarketOrder(String instrument, int tradeSize, OrderDetails details) {
        validateAccount(accountId);

        // Place market order
        try {
            OrderCreateRequest orderCreateRequest = new OrderCreateRequest(accountId);

            MarketOrderRequest marketOrderRequest = getMarketOrderRequest(instrument, tradeSize, details);
            orderCreateRequest.setOrder(marketOrderRequest);

            // Place trade
            OrderCreateResponse response = ctx.order.create(orderCreateRequest);
            if (response.getOrderFillTransaction() == null || response.getOrderFillTransaction().getTradeOpened() == null) {
                // Log in SL4J
                return null; // Trade wasn't filled
            }
            OrderFillTransaction transaction = response.getOrderFillTransaction();

            return new FilledTradeDetails(
                    transaction.getTradeOpened().getTradeID().toString(),
                    transaction.getTradeOpened().getPrice().bigDecimalValue(),
                    Instant.parse(transaction.getTime()),
                    transaction.getTradeOpened().getUnits().bigDecimalValue()
            );
        } catch (RequestException | ExecuteException e) {
            throw new BrokerException("Unable to place market order for " + instrument, e);
        }
    }

    MarketOrderRequest getMarketOrderRequest(String instrument, int tradeSize, OrderDetails details) {
        // Build stop loss and take profit details if necessary
        StopLossDetails stopLossDetails = getStopLossDetails(details);
        TrailingStopLossDetails trailingStopLossDetails = getTrailingStopLossDetails(details);
        TakeProfitDetails takeProfitDetails = getTakeProfitDetails(details);

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
        return marketOrderRequest;
    }

    // Builds Oanda stop loss details, based on provided distance
    private StopLossDetails getStopLossDetails(OrderDetails details) {
        // Return null if no stop loss or trailing stop loss is specified
        if (details.getStopLossDistance() == null || details.getIsTrailingStopLoss() ) return null;

        StopLossDetails stopLossDetails = new StopLossDetails();
        stopLossDetails.setDistance(details.getStopLossDistance());
        return stopLossDetails;
    }

    // Builds Oanda trailing stop loss details, based on provided distance
    private TrailingStopLossDetails getTrailingStopLossDetails(OrderDetails details) {
        // Return null if  no stop loss or normal stop loss is specified
        if (details.getStopLossDistance() == null || !details.getIsTrailingStopLoss()) return null;

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

    // Closes a position with the given trade id and instrument
    @Override
    public FilledTradeDetails closePosition(String instrument, String tradeId) {
        validateAccount(accountId);

        try {
            TradeCloseResponse closeResponse = ctx.trade.close(new TradeCloseRequest(accountId, new TradeSpecifier(tradeId)));
            if (closeResponse == null || closeResponse.getOrderFillTransaction() == null
                    || closeResponse.getOrderFillTransaction().getTradesClosed() == null
                    || closeResponse.getOrderFillTransaction().getTradesClosed().isEmpty()) {
                // Log in SL4J
                return null; // Trade wasn't closed
            }
            TradeReduce trade = closeResponse.getOrderFillTransaction().getTradesClosed().getFirst();

            return new FilledTradeDetails(
                    trade.getTradeID().toString(),
                    trade.getPrice().bigDecimalValue(),
                    Instant.parse(closeResponse.getOrderFillTransaction().getTime()),
                    trade.getUnits().bigDecimalValue()
            );
        } catch (RequestException | ExecuteException e) {
            throw new BrokerException("Unable to close position for " + instrument, e);
        }
    }

    // Gets the close price of the instrument in USD, used for forex trading
    @Override
    public BigDecimal getClosePriceInUsd(String instrument) {
        ArrayList<String> instruments = new ArrayList<>();
        instruments.add(instrument);
        PricingGetRequest req = new PricingGetRequest(accountId, instruments);

        try {
            PricingGetResponse resp = ctx.pricing.get(req);
            ClientPrice price = resp.getPrices().getFirst();
            return BigDecimal.valueOf(price.getQuoteHomeConversionFactors().getPositiveUnits().doubleValue());
        }  catch (RequestException | ExecuteException e) {
            throw new BrokerException("Unable retrieve close price in USD for " + instrument, e);
        }
    }

    @Override
    public TradeStatusDetails getTradeStatus(String tradeId) {
        try {
            TradeGetResponse tradeResponse = ctx.trade.get(accountId, new TradeSpecifier(tradeId));
            Trade trade = tradeResponse.getTrade();
            // Determine if trade is open or closed (close when tradeable is recognized as open, and will be reconciled when closed)
            if (trade.getState() == TradeState.OPEN || trade.getState() == TradeState.CLOSE_WHEN_TRADEABLE) {
                // Return the wrapper, only containing the open trade status
                return new TradeStatusDetails(TradeStatusDetails.Status.OPEN);
                // Log unrealize pnl in SL4J
            }

            // Get the close reason
            TransactionID closingTransactionId = trade.getClosingTransactionIDs().getFirst(); // Trades will be closed in one transaction
            TransactionGetResponse transactionResponse = ctx.transaction.get(accountId, closingTransactionId);

            TradeCloseReason closeReason = TradeCloseReason.UNKNOWN;
            if (transactionResponse.getTransaction() instanceof OrderFillTransaction fill) {
                OrderFillReason reason = fill.getReason();
                switch (reason) {
                    case OrderFillReason.TAKE_PROFIT_ORDER:
                        closeReason = TradeCloseReason.TAKE_PROFIT;
                        break;
                    case OrderFillReason.STOP_LOSS_ORDER:
                        closeReason = TradeCloseReason.STOP_LOSS;
                        break;
                    case OrderFillReason.TRAILING_STOP_LOSS_ORDER:
                        closeReason = TradeCloseReason.TRAILING_STOP_LOSS;
                        break;
                    default:
                        System.out.println("Unexpected close reason occurred: " + reason); // Temporary until SL4J is implemented
                        // Log in SL4J that an unexpected trade close reason occurred, along with the specific reason
                        // however continue with close reason as null, to make sure the trade is closed in the database
                }
            }

            // Build Filled Trade Details
            FilledTradeDetails filledTradeDetails = new FilledTradeDetails(
                    tradeId,
                    trade.getAverageClosePrice().bigDecimalValue(),
                    Instant.parse(trade.getCloseTime()),
                    trade.getInitialUnits().bigDecimalValue()
            );

            return new TradeStatusDetails(TradeStatusDetails.Status.CLOSED, closeReason, filledTradeDetails);
        } catch (RequestException | ExecuteException e) {
            throw new BrokerException("Error retrieving trade status from Oanda: " + tradeId, e);
        }

    }

    // Helper method used for placing trades. Ensures the account exists and has a non-zero balance.
    private void validateAccount(AccountID accountId) {
        // Ensure account exists and  has non-zero balance
        try {
            // Execute the request and retrieve a response object
            AccountGetResponse response = ctx.account.get(accountId);
            if (response == null || response.getAccount() == null) throw new BrokerException("Account not found in current broker context");

            // Retrieve the contents of the result
            Account account = response.getAccount();
            // Check the balance
            if (account.getBalance().doubleValue() <= 0.0) throw new BrokerException("Account balance <= 0");
        } catch (RequestException | ExecuteException e) {
            throw new BrokerException("Error retrieving account from Oanda", e);
        }
    }

    // Helper method that parses the time returned from Oanda for a given candle/bar
    ZonedDateTime getOandaEndTime(Candlestick candle) {
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
        // Build OffsetDateTime for Chicago
        ZoneId chicagoZone = ZoneId.of("America/Chicago");
        // Build ZonedDateTime and return
        return localDateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(chicagoZone);
    }

    @Override
    public String getBrokerName() {
        return "Oanda";
    }

}
