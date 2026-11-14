package bot.broker;

import bot.testutils.CandlestickLoader;
import com.oanda.v20.Context;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.account.*;
import com.oanda.v20.instrument.*;
import com.oanda.v20.order.*;
import com.oanda.v20.primitives.AccountUnits;
import com.oanda.v20.trade.*;
import com.oanda.v20.transaction.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OandaBrokerTest {

    @Mock Context mockCtx;
    @Mock AccountID mockAccountId;
    @Mock AccountGetResponse mockAccountGetResponse;
    @Mock AccountContext mockAccountContext;
    @Mock Account mockAccount;
    @Mock AccountUnits mockAccountUnits;

    @Mock InstrumentContext mockInstrumentContext;
    @Mock InstrumentCandlesResponse mockInstrumentCandlesResponse;

    @Mock OrderContext mockOrderContext;
    @Mock OrderCreateResponse mockOrderCreateResponse;

    @Mock TradeContext mockTradeContext;
    @Mock TradeCloseResponse mockTradeCloseResponse;

    @Mock TradeGetResponse mockTradeGetResponse;
    @Mock TransactionContext mockTransactionContext;
    @Mock TransactionGetResponse mockTransactionGetResponse;

    // getHistoricalBarSeries tests
    @Test
    void getHistoricalBarSeriesTest() throws ExecuteException, RequestException {
        List<Candlestick> responseCandles = CandlestickLoader.loadCandlesticksFromCSV("/eurusd_daily.csv");

        mockCtx.instrument = mockInstrumentContext;
        when(mockCtx.instrument.candles(any(InstrumentCandlesRequest.class))).thenReturn(mockInstrumentCandlesResponse);
        when(mockInstrumentCandlesResponse.getCandles()).thenReturn(responseCandles);

        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        BarSeries series = testBroker.getHistoricalBarSeries("EUR_USD");

        // Verify the number of bars
        assertEquals(250, series.getBarCount());

        // Verify the most recent bar
        assertEquals("2026-11-02T23:00:00Z", series.getLastBar().getEndTime().toString());
        assertEquals(1.11886, series.getLastBar().getOpenPrice().doubleValue());
        assertEquals(1.12067, series.getLastBar().getHighPrice().doubleValue());
        assertEquals(1.11752, series.getLastBar().getLowPrice().doubleValue());
        assertEquals(1.11826, series.getLastBar().getClosePrice().doubleValue());
        assertEquals(69182, series.getLastBar().getVolume().doubleValue());
    }

    @Test
    void getHistoricalBarSeriesExceptionTest() throws ExecuteException, RequestException {
        // Throw mock error when sending the candle request
        mockCtx.instrument = mockInstrumentContext;
        when(mockCtx.instrument.candles(any(InstrumentCandlesRequest.class))).thenThrow(new ExecuteException(
                "Execution failed due to an unhandled downstream broker response while fetching historical bars.",
                new Throwable("java.util.concurrent.TimeoutException: Remote execution timed out waiting for OANDA historical data engine response after 15000ms.")));

        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);

        // Verify the execute exception is caught and a broker exception is thrown
        BrokerException brokerException = assertThrows(BrokerException.class, () -> testBroker.getHistoricalBarSeries("EUR_USD"));
        assertEquals("Unable to retrieve historical bar series for EUR_USD", brokerException.getMessage());
    }

    // getLatestBar tests
    @Test
    void getLatestBarTest() throws ExecuteException, RequestException {
        List<Candlestick> responseCandles = CandlestickLoader.loadCandlesticksFromCSV("/eurusd_daily.csv");

        mockCtx.instrument = mockInstrumentContext;
        when(mockCtx.instrument.candles(any(InstrumentCandlesRequest.class))).thenReturn(mockInstrumentCandlesResponse);
        when(mockInstrumentCandlesResponse.getCandles()).thenReturn(responseCandles);

        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        BaseBar latestBar = testBroker.getLatestBar("EUR_USD");

        // Verify the most recent bar
        assertEquals("2026-11-02T23:00:00Z", latestBar.getEndTime().toString());
        assertEquals(1.11886, latestBar.getOpenPrice().doubleValue());
        assertEquals(1.12067, latestBar.getHighPrice().doubleValue());
        assertEquals(1.11752, latestBar.getLowPrice().doubleValue());
        assertEquals(1.11826, latestBar.getClosePrice().doubleValue());
        assertEquals(69182, latestBar.getVolume().doubleValue());
    }

    @Test
    void getLatestBarExceptionTest() throws ExecuteException, RequestException {
        // Throw mock error when sending the candle request
        mockCtx.instrument = mockInstrumentContext;
        when(mockCtx.instrument.candles(any(InstrumentCandlesRequest.class))).thenThrow(new ExecuteException(
                "Execution failed due to an unhandled downstream broker response while fetching historical bars.",
                new Throwable("java.util.concurrent.TimeoutException: Remote execution timed out waiting for OANDA historical data engine response after 15000ms.")));

        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);

        // Verify the execute exception is caught and a broker exception is thrown
        BrokerException brokerException = assertThrows(BrokerException.class, () -> testBroker.getLatestBar("EUR_USD"));
        assertEquals("Unable to retrieve latest bar for EUR_USD", brokerException.getMessage());
    }

    // placeMarketOrder tests
    @Test
    void placeMarketOrderTest() throws ExecuteException, RequestException {
        // Setup stubs for validating account and placing the order
        mockValidateAccount(10000.0);
        mockPlaceOrder("00001", 1.11826, "2026-11-02T23:00:00Z", 10, true);

        // Create broker and place order
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        OrderDetails orderDetails = new OrderDetails();
        FilledTradeDetails filledTradeDetails = testBroker.placeMarketOrder("EUR_USD", 10, orderDetails);

        // Verify the trade details
        assertEquals("00001", filledTradeDetails.getTradeId());
        assertEquals(1.11826, filledTradeDetails.getFillPrice().doubleValue());
        assertEquals("2026-11-02T23:00:00Z", filledTradeDetails.getFillDate().toString());
        assertEquals(10, filledTradeDetails.getUnits().doubleValue());
    }

    @Test
    void placeMarketOrderNoTradeFilledTest() throws ExecuteException, RequestException {
        mockValidateAccount(10000.0);
        mockPlaceOrder("00001", 1.11826, "2026-11-02T23:00:00Z", 10, false);

        // Verify that null is returned if the trade is not opened
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        OrderDetails orderDetails = new OrderDetails();
        assertNull(testBroker.placeMarketOrder("EUR_USD", 10, orderDetails));
    }

    @Test
    void placeMarketOrderExceptionTest() throws ExecuteException, RequestException {
        mockValidateAccount(10000.0);

        // Stub error
        mockCtx.order = mockOrderContext;
        when(mockCtx.order.create(any(OrderCreateRequest.class))).thenThrow(new ExecuteException(
                "Execution failed due to an unhandled downstream broker response while placing market order.",
                new Throwable("java.util.concurrent.TimeoutException: Remote execution timed out waiting for OANDA order response after 15000ms.")));

        // Create broker and place order
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        OrderDetails orderDetails = new OrderDetails();

        // Verify the execute exception is caught and a broker exception is thrown
        BrokerException brokerException = assertThrows(BrokerException.class,
                () -> testBroker.placeMarketOrder("EUR_USD", 10, orderDetails));
        assertEquals("Unable to place market order for EUR_USD", brokerException.getMessage());
    }

    @Test
    void getMarketOrderRequestTakeProfitTest() {
        // Create broker and order details
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        OrderDetails orderDetails = new OrderDetails();
        orderDetails.setTakeProfitPrice(new BigDecimal("1.11926"));

        // Verify Market Order Request
        MarketOrderRequest marketOrderRequest = testBroker.getMarketOrderRequest("EUR_USD", 10, orderDetails);
        assertEquals("EUR_USD", marketOrderRequest.getInstrument().toString());
        assertEquals(10, marketOrderRequest.getUnits().doubleValue());
        assertEquals(1.11926, marketOrderRequest.getTakeProfitOnFill().getPrice().doubleValue());
        assertNull(marketOrderRequest.getStopLossOnFill());
        assertNull(marketOrderRequest.getTrailingStopLossOnFill());
    }

    @Test
    void getMarketOrderRequestStopLossTest() {
        // Create broker and order details
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        OrderDetails orderDetails = new OrderDetails();
        orderDetails.setStopLoss(new BigDecimal("0.001"), false);

        // Verify Market Order Request
        MarketOrderRequest marketOrderRequest = testBroker.getMarketOrderRequest("EUR_USD", 10, orderDetails);
        assertEquals("EUR_USD", marketOrderRequest.getInstrument().toString());
        assertEquals(10, marketOrderRequest.getUnits().doubleValue());
        assertEquals(0.001, marketOrderRequest.getStopLossOnFill().getDistance().doubleValue());
        assertNull(marketOrderRequest.getTakeProfitOnFill());
        assertNull(marketOrderRequest.getTrailingStopLossOnFill());

    }

    @Test
    void getMarketOrderRequestTrailingStopLossTest() {
        // Create broker and place order
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        OrderDetails orderDetails = new OrderDetails();
        orderDetails.setStopLoss(new BigDecimal("0.001"), true);

        // Verify Market Order Request
        MarketOrderRequest marketOrderRequest = testBroker.getMarketOrderRequest("EUR_USD", 10, orderDetails);
        assertEquals("EUR_USD", marketOrderRequest.getInstrument().toString());
        assertEquals(10, marketOrderRequest.getUnits().doubleValue());
        assertEquals(0.001, marketOrderRequest.getTrailingStopLossOnFill().getDistance().doubleValue());
        assertNull(marketOrderRequest.getTakeProfitOnFill());
        assertNull(marketOrderRequest.getStopLossOnFill());
    }

    // closePosition tests
    @Test
    void closePositionTest() throws ExecuteException, RequestException {
        mockValidateAccount(10000);
        mockClosePosition("00001", 1.11926, "2026-11-03T23:00:00Z", 10, true);

        // Create broker and close position
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        FilledTradeDetails filledTradeDetails = testBroker.closePosition("EUR_USD", "00001");

        // Verify trade details
        assertEquals("00001", filledTradeDetails.getTradeId());
        assertEquals(1.11926, filledTradeDetails.getFillPrice().doubleValue());
        assertEquals("2026-11-03T23:00:00Z", filledTradeDetails.getFillDate().toString());
        assertEquals(10, filledTradeDetails.getUnits().doubleValue());
    }

    @Test
    void closePositionNotClosedTest() throws ExecuteException, RequestException {
        mockValidateAccount(10000);
        mockClosePosition("00001", 1.11926, "2026-11-03T23:00:00Z", 10, false);

        // Create broker, attempt to close position, and verify the position was not closed
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        assertNull(testBroker.closePosition("EUR_USD", "00001"));
    }

    @Test
    void closePositionExceptionTest() throws ExecuteException, RequestException {
        mockValidateAccount(10000.0);

        // Stub exception
        mockCtx.trade = mockTradeContext;
        when(mockCtx.trade.close(any(TradeCloseRequest.class))).thenThrow(new ExecuteException(
                "Execution failed due to an unhandled downstream broker response while closing position.",
                new Throwable("java.util.concurrent.TimeoutException: Remote execution timed out waiting for OANDA order response after 15000ms.")));

        // Create broker and close position
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);

        // Verify the execute exception is caught and a broker exception is thrown
        BrokerException brokerException = assertThrows(BrokerException.class,
                () -> testBroker.closePosition("EUR_USD", "00001"));
        assertEquals("Unable to close position for EUR_USD", brokerException.getMessage());
    }

    // getTradeStatus tests
    @Test
    void getTradeStatusOpenTest() throws ExecuteException, RequestException {
        // Mock the trade status
        String tradeId = "00001";
        mockCtx.trade = mockTradeContext;
        TradeSpecifier tradeSpecifier = new TradeSpecifier(tradeId);
        when(mockTradeContext.get(mockAccountId, tradeSpecifier)).thenReturn(mockTradeGetResponse);
        Trade trade = new Trade();
        trade.setState(TradeState.OPEN);
        trade.setId(tradeId);
        when(mockTradeGetResponse.getTrade()).thenReturn(trade);

        // Create Broker and call getTradeStatus
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        TradeStatusDetails details = testBroker.getTradeStatus(tradeId);
        assertEquals(tradeId, details.getTradeId());
        assertEquals(TradeStatusDetails.Status.OPEN, details.getTradeStatus());
    }

    @Test
    void getTradeStatusClosedTakeProfitTest() throws ExecuteException, RequestException {
        // Mock the trade status
        String tradeId = "00001";
        mockCtx.trade = mockTradeContext;
        TradeSpecifier tradeSpecifier = new TradeSpecifier(tradeId);
        when(mockTradeContext.get(mockAccountId, tradeSpecifier)).thenReturn(mockTradeGetResponse);
        Trade trade = new Trade();
        trade.setState(TradeState.CLOSED);
        trade.setId(tradeId);
        trade.setClosingTransactionIDs(new ArrayList<>(List.of(new TransactionID("txn_01"))));
        trade.setAverageClosePrice(1.14152);
        trade.setCloseTime("2026-11-03T23:00:00Z");
        trade.setInitialUnits(new BigDecimal("10"));
        when(mockTradeGetResponse.getTrade()).thenReturn(trade);

        // mock transaction get response
        mockCtx.transaction = mockTransactionContext;
        when(mockTransactionContext.get(mockAccountId, trade.getClosingTransactionIDs().getFirst())).thenReturn(mockTransactionGetResponse);
        OrderFillTransaction fillTransaction = new OrderFillTransaction();
        fillTransaction.setReason(OrderFillReason.TAKE_PROFIT_ORDER);
        when(mockTransactionGetResponse.getTransaction()).thenReturn(fillTransaction);

        // Create Broker and call getTradeStatus
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        TradeStatusDetails details = testBroker.getTradeStatus(tradeId);
        assertEquals(tradeId, details.getTradeId());
        assertEquals(TradeStatusDetails.Status.CLOSED, details.getTradeStatus());
        assertEquals(TradeCloseReason.TAKE_PROFIT, details.getCloseReason());
        assertEquals("2026-11-03T23:00:00Z", details.getFilledTradeDetails().getFillDate().toString());
        assertEquals(10, details.getFilledTradeDetails().getUnits().doubleValue());
    }

    @Test
    void getTradeStatusClosedStopLossTest() throws ExecuteException, RequestException {
        // Mock the trade status
        String tradeId = "00001";
        mockCtx.trade = mockTradeContext;
        TradeSpecifier tradeSpecifier = new TradeSpecifier(tradeId);
        when(mockTradeContext.get(mockAccountId, tradeSpecifier)).thenReturn(mockTradeGetResponse);
        Trade trade = new Trade();
        trade.setState(TradeState.CLOSED);
        trade.setId(tradeId);
        trade.setClosingTransactionIDs(new ArrayList<>(List.of(new TransactionID("txn_01"))));
        trade.setAverageClosePrice(1.14152);
        trade.setCloseTime("2026-11-03T23:00:00Z");
        trade.setInitialUnits(new BigDecimal("10"));
        when(mockTradeGetResponse.getTrade()).thenReturn(trade);

        // mock transaction get response
        mockCtx.transaction = mockTransactionContext;
        when(mockTransactionContext.get(mockAccountId, trade.getClosingTransactionIDs().getFirst())).thenReturn(mockTransactionGetResponse);
        OrderFillTransaction fillTransaction = new OrderFillTransaction();
        fillTransaction.setReason(OrderFillReason.STOP_LOSS_ORDER);
        when(mockTransactionGetResponse.getTransaction()).thenReturn(fillTransaction);

        // Create Broker and call getTradeStatus
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        TradeStatusDetails details = testBroker.getTradeStatus(tradeId);
        assertEquals(TradeCloseReason.STOP_LOSS, details.getCloseReason());
    }

    @Test
    void getTradeStatusClosedTrailingStopLossTest() throws ExecuteException, RequestException {
        // Mock the trade status
        String tradeId = "00001";
        mockCtx.trade = mockTradeContext;
        TradeSpecifier tradeSpecifier = new TradeSpecifier(tradeId);
        when(mockTradeContext.get(mockAccountId, tradeSpecifier)).thenReturn(mockTradeGetResponse);
        Trade trade = new Trade();
        trade.setState(TradeState.CLOSED);
        trade.setId(tradeId);
        trade.setClosingTransactionIDs(new ArrayList<>(List.of(new TransactionID("txn_01"))));
        trade.setAverageClosePrice(1.14152);
        trade.setCloseTime("2026-11-03T23:00:00Z");
        trade.setInitialUnits(new BigDecimal("10"));
        when(mockTradeGetResponse.getTrade()).thenReturn(trade);

        // mock transaction get response
        mockCtx.transaction = mockTransactionContext;
        when(mockTransactionContext.get(mockAccountId, trade.getClosingTransactionIDs().getFirst())).thenReturn(mockTransactionGetResponse);
        OrderFillTransaction fillTransaction = new OrderFillTransaction();
        fillTransaction.setReason(OrderFillReason.TRAILING_STOP_LOSS_ORDER);
        when(mockTransactionGetResponse.getTransaction()).thenReturn(fillTransaction);

        // Create Broker and call getTradeStatus
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        TradeStatusDetails details = testBroker.getTradeStatus(tradeId);
        assertEquals(TradeCloseReason.TRAILING_STOP_LOSS, details.getCloseReason());
    }

    @Test
    void getTradeStatusClosedUnknownTest() throws ExecuteException, RequestException {
        // Mock the trade status
        String tradeId = "00001";
        mockCtx.trade = mockTradeContext;
        TradeSpecifier tradeSpecifier = new TradeSpecifier(tradeId);
        when(mockTradeContext.get(mockAccountId, tradeSpecifier)).thenReturn(mockTradeGetResponse);
        Trade trade = new Trade();
        trade.setState(TradeState.CLOSED);
        trade.setId(tradeId);
        trade.setClosingTransactionIDs(new ArrayList<>(List.of(new TransactionID("txn_01"))));
        trade.setAverageClosePrice(1.14152);
        trade.setCloseTime("2026-11-03T23:00:00Z");
        trade.setInitialUnits(new BigDecimal("10"));
        when(mockTradeGetResponse.getTrade()).thenReturn(trade);

        // mock transaction get response
        mockCtx.transaction = mockTransactionContext;
        when(mockTransactionContext.get(mockAccountId, trade.getClosingTransactionIDs().getFirst())).thenReturn(mockTransactionGetResponse);
        OrderFillTransaction fillTransaction = new OrderFillTransaction();
        fillTransaction.setReason(OrderFillReason.MARKET_ORDER);
        when(mockTransactionGetResponse.getTransaction()).thenReturn(fillTransaction);

        // Create Broker and call getTradeStatus
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        TradeStatusDetails details = testBroker.getTradeStatus(tradeId);
        assertEquals(TradeCloseReason.UNKNOWN, details.getCloseReason());
    }

    @Test
    void getTradeStatusExceptionTest() throws ExecuteException, RequestException {
        // Mock the trade status
        String tradeId = "00001";
        mockCtx.trade = mockTradeContext;
        TradeSpecifier tradeSpecifier = new TradeSpecifier(tradeId);
        when(mockTradeContext.get(mockAccountId, tradeSpecifier)).thenReturn(mockTradeGetResponse);
        Trade trade = new Trade();
        trade.setState(TradeState.CLOSED);
        trade.setId(tradeId);
        // Leave closing transaction ids as null to trigger a null pointer exception
        when(mockTradeGetResponse.getTrade()).thenReturn(trade);

        // Create Broker and call getTradeStatus
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        assertThrows(BrokerException.class, () -> testBroker.getTradeStatus(tradeId));
    }

    // validateAccount Tests, mostly just testing exceptions, success cases covered by others
    @Test
    void validateAccountExecuteException() throws ExecuteException, RequestException {
        // Stub exception
        mockCtx.account = mockAccountContext;
        when(mockCtx.account.get(mockAccountId)).thenThrow(new ExecuteException(
                "Execution failed due to an unhandled downstream broker response while retrieving account.",
                new Throwable("java.util.concurrent.TimeoutException: Remote execution timed out waiting for OANDA response after 15000ms.")));

        // Create Broker and call place order to trigger exception
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        OrderDetails orderDetails = new OrderDetails();
        BrokerException brokerException = assertThrows(BrokerException.class,
                () -> testBroker.placeMarketOrder("EUR_USD", 10, orderDetails));

        // Verify exception message
        assertEquals("Error retrieving account from Oanda", brokerException.getMessage());
    }

    @Test
    void validateAccountMissingAccountException() throws ExecuteException, RequestException {
        mockCtx.account = mockAccountContext;
        when(mockCtx.account.get(mockAccountId)).thenReturn(mockAccountGetResponse);
        when(mockAccountGetResponse.getAccount()).thenReturn(null);

        // Create Broker and call place order to trigger exception
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        OrderDetails orderDetails = new OrderDetails();
        BrokerException brokerException = assertThrows(BrokerException.class,
                () -> testBroker.placeMarketOrder("EUR_USD", 10, orderDetails));

        // Verify exception message
        assertEquals("Account not found in current broker context", brokerException.getMessage());
    }

    @Test
    void validateAccountZeroBalanceException() throws ExecuteException, RequestException {
        mockValidateAccount(0);

        // Create Broker and call place order to trigger exception
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        OrderDetails orderDetails = new OrderDetails();
        BrokerException brokerException = assertThrows(BrokerException.class,
                () -> testBroker.placeMarketOrder("EUR_USD", 10, orderDetails));

        // Verify exception message
        assertEquals("Account balance <= 0", brokerException.getMessage());
    }

    @Test
    void validateAccountNegativeBalanceException() throws ExecuteException, RequestException {
        mockValidateAccount(-100.0);

        // Create Broker and call place order to trigger exception
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        OrderDetails orderDetails = new OrderDetails();
        BrokerException brokerException = assertThrows(BrokerException.class,
                () -> testBroker.placeMarketOrder("EUR_USD", 10, orderDetails));

        // Verify exception message
        assertEquals("Account balance <= 0", brokerException.getMessage());
    }


    // getOandaEndTime tests
    @Test
    void getOandaEndTimeStandardTimeTest() {
        Candlestick candle = new Candlestick();
        candle.setTime("2026-11-03T05:00:00.000000000Z");

        // Create Broker and get end time
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        ZonedDateTime result = testBroker.getOandaEndTime(candle);

        assertEquals("2026-11-02T23:00", result.toLocalDateTime().toString());
        assertEquals("-06:00", result.getOffset().toString());
        assertEquals("America/Chicago", result.getZone().getId());
    }

    @Test
    void getOandaEndTimeDaylightTimeTest() {
        Candlestick candle = new Candlestick();
        candle.setTime("2026-10-20T05:00:00.000000000Z");

        // Create Broker and get end time
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        ZonedDateTime result = testBroker.getOandaEndTime(candle);

        assertEquals("2026-10-20T00:00", result.toLocalDateTime().toString());
        assertEquals("-05:00", result.getOffset().toString());
        assertEquals("America/Chicago", result.getZone().getId());
    }

    @Test
    void getOandaEndTimeDaylightToStandardTest() {
        Candlestick candle = new Candlestick();
        candle.setTime("2026-11-01T07:30:00.000000Z");

        // Create Broker and get end time
        OandaBroker testBroker = new OandaBroker(mockCtx, mockAccountId);
        ZonedDateTime result = testBroker.getOandaEndTime(candle);

        assertEquals("2026-11-01T01:30", result.toLocalDateTime().toString());
        assertEquals("-06:00", result.getOffset().toString());
        assertEquals("America/Chicago", result.getZone().getId());
    }


    // Helper methods, used to set up common mockito stubs
    private void mockValidateAccount(double balance) throws ExecuteException, RequestException {
        // Mock ensure account exists and balance is > 0
        mockCtx.account = mockAccountContext;
        when(mockCtx.account.get(mockAccountId)).thenReturn(mockAccountGetResponse);
        when(mockAccountGetResponse.getAccount()).thenReturn(mockAccount);
        when(mockAccount.getBalance()).thenReturn(mockAccountUnits);
        when(mockAccountUnits.doubleValue()).thenReturn(balance);
    }

    private void mockPlaceOrder(String tradeId, double price, String openTime, double units, boolean tradeOpened) throws ExecuteException, RequestException {
        mockCtx.order = mockOrderContext;
        OrderFillTransaction orderFillTransaction = new OrderFillTransaction();
        TradeOpen tradeOpen = new TradeOpen();

        tradeOpen.setTradeID(tradeId);
        tradeOpen.setPrice(price);
        tradeOpen.setUnits(units);
        orderFillTransaction.setTime(openTime);

        if (tradeOpened) {
            orderFillTransaction.setTradeOpened(tradeOpen);
        }

        when(mockCtx.order.create(any(OrderCreateRequest.class))).thenReturn(mockOrderCreateResponse);
        when(mockOrderCreateResponse.getOrderFillTransaction()).thenReturn(orderFillTransaction);
    }

    private void mockClosePosition(String tradeId, double closePrice, String closeTime, double units, boolean tradeClosed) throws ExecuteException, RequestException {
        mockCtx.trade = mockTradeContext;
        OrderFillTransaction orderFillTransaction = new OrderFillTransaction();
        TradeReduce tradeReduce = new TradeReduce();

        tradeReduce.setTradeID(tradeId);
        tradeReduce.setPrice(closePrice);
        tradeReduce.setUnits(units);
        orderFillTransaction.setTime(closeTime);

        if (tradeClosed) {
            orderFillTransaction.setTradesClosed(new ArrayList<>(List.of(tradeReduce)));
        }

        when(mockCtx.trade.close(any(TradeCloseRequest.class))).thenReturn(mockTradeCloseResponse);
        when(mockTradeCloseResponse.getOrderFillTransaction()).thenReturn(orderFillTransaction);
    }
}
