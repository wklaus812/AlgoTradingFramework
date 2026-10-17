package main.engine;

import java.math.BigDecimal;
import java.time.Instant;

public class FilledTradeDetails {

    // Instead of returning just the trade id, let's build and return a wrapper, similar to the details wrappers
    // Class: FilledTradeDetails
    // Fields: TradeId, FillPrice, FillDate, units/tradesize
    private final String tradeId;
    private final BigDecimal fillPrice;
    private final Instant fillDate;
    private final BigDecimal units;

    public FilledTradeDetails(String tradeId, BigDecimal fillPrice, Instant fillDate, BigDecimal units) {
        this.tradeId = tradeId;
        this.fillPrice = fillPrice;
        this.fillDate = fillDate;
        this.units = units;
    }

    public String getTradeId() {return tradeId;}
    public BigDecimal getFillPrice() {return fillPrice;}
    public Instant getFillDate() {return fillDate;}
    public BigDecimal getUnits() {return units;}

}
