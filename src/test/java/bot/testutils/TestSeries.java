package bot.testutils;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;

public class TestSeries {
    private static final Instant START = Instant.parse("2026-01-01T21:00:00Z");

    private TestSeries() {}

    public static BarSeries trending(int count, double startPrice, double drift, double range) {
        BarSeries series = new BaseBarSeriesBuilder().withName("test").build();
        double prevClose = startPrice;
        for (int i = 0; i < count; i++) {
            double close = startPrice + drift * i;
            series.addBar(bar(START.plus(Duration.ofDays(i)), prevClose, close, range));
            prevClose = close;
        }
        return series;
    }

    public static BarSeries flat(int count, double price, double range) {
        return trending(count, price, 0, range);
    }

    // Returns the next day bar in the series based on the provided parameters
    public static Bar nextBar(BarSeries series, double close, double range) {
        Instant next = series.getLastBar().getEndTime().plus(Duration.ofDays(1));
        double open = series.getLastBar().getClosePrice().doubleValue();
        return bar(next, open, close, range);
    }

    // Returns a same day replacement bar based on the provided parameters
    public static Bar sameDayBar(BarSeries series, double close, double range) {
        Instant same = series.getLastBar().getEndTime();
        double open = series.getLastBar().getOpenPrice().doubleValue();
        return bar(same, open, close, range);
    }

    private static Bar bar(Instant end, double open, double close, double range) {
        double high = round(Math.max(open, close) + range / 2);
        double low = round(Math.min(open, close) - range / 2);
        return new BaseBar(Duration.ofDays(1), null, end, DecimalNum.valueOf(round(open)), DecimalNum.valueOf(high), DecimalNum.valueOf(low), DecimalNum.valueOf(round(close)),
                DecimalNum.valueOf(1000), null, 0);
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(5, RoundingMode.HALF_UP).doubleValue();
    }

}
