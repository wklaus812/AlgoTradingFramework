package bot.engine;

import java.math.BigDecimal;

public class OrderDetails {
    private Boolean isTrailingStopLoss = false;
    private BigDecimal stopLossDistance = null;
    private BigDecimal takeProfitPrice = null;

    public OrderDetails() {}

    public void setStopLoss(BigDecimal stopLossDistance, Boolean isTrailingStopLoss) {
        this.stopLossDistance = stopLossDistance;
        this.isTrailingStopLoss = isTrailingStopLoss;
    }

    public void setTakeProfitPrice(BigDecimal price) {
        this.takeProfitPrice = price;
    }

    public Boolean getIsTrailingStopLoss() {
        return isTrailingStopLoss;
    }

    public BigDecimal getStopLossDistance() {
        return stopLossDistance;
    }

    public BigDecimal getTakeProfitPrice() {
        return takeProfitPrice;
    }


}