package bot.broker;

public class TradeStatusDetails {

    public enum Status {
        OPEN,
        CLOSED
    }

    private Status tradeStatus;
    private TradeCloseReason closeReason;
    private FilledTradeDetails filledTradeDetails;

    public TradeStatusDetails(TradeStatusDetails.Status status) {
        tradeStatus = status;
        closeReason = null;
        filledTradeDetails = null;
    }

    public TradeStatusDetails(TradeStatusDetails.Status status, TradeCloseReason reason, FilledTradeDetails details) {
        tradeStatus = status;
        closeReason = reason;
        filledTradeDetails = details;
    }

    public Status getTradeStatus() {
        return tradeStatus;
    }

    public TradeCloseReason getCloseReason() {
        return closeReason;
    }

    public FilledTradeDetails getFilledTradeDetails() {
        return filledTradeDetails;
    }
}
