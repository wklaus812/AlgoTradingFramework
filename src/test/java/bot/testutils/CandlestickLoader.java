package bot.testutils;

import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickData;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CandlestickLoader {

    public static List<Candlestick> loadCandlesticksFromCSV(String filePath) {
        try (InputStream inputStream = CandlestickLoader.class.getResourceAsStream(filePath)) {
            List<Candlestick> candlesticks = new ArrayList<>();
            if (inputStream != null) {
                java.io.BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    candlesticks.add(getCandlestick(line));
                }
            }
            return candlesticks;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Candlestick getCandlestick(String line) {
        String[] splitLine = line.split(",");
        Candlestick newCandlestick = new Candlestick();
        newCandlestick.setTime(splitLine[0].replace("\"", ""));
        CandlestickData mid = new CandlestickData();
        mid.setO(splitLine[1].replace("\"", ""));
        mid.setH(splitLine[2].replace("\"", ""));
        mid.setL(splitLine[3].replace("\"", ""));
        mid.setC(splitLine[4].replace("\"", ""));
        newCandlestick.setMid(mid);
        newCandlestick.setVolume(Long.parseLong(splitLine[5].replace("\"", "")));
        return newCandlestick;
    }
}
