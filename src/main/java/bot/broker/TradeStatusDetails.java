package bot.broker;

public class TradeStatusDetails {

    public enum Status {
        OPEN,
        CLOSED
    }

    private String tradeId;
    private Status tradeStatus;
    private TradeCloseReason closeReason;
    private FilledTradeDetails filledTradeDetails;

    public TradeStatusDetails(String tradeId, TradeStatusDetails.Status status) {
        this.tradeId = tradeId;
        this.tradeStatus = status;
        this.closeReason = null;
        this.filledTradeDetails = null;
    }

    public TradeStatusDetails(String tradeId, TradeStatusDetails.Status status, TradeCloseReason reason, FilledTradeDetails details) {
        this.tradeId = tradeId;
        this.tradeStatus = status;
        this.closeReason = reason;
        this.filledTradeDetails = details;
    }

    public String getTradeId() {
        return tradeId;
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
