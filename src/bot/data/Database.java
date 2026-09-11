package bot.data;

import javax.xml.transform.Result;
import java.sql.*;

public class Database {
    private final static String url = "jdbc:sqlite:TradeHistory";

    private final static String createTradeHistoryTable = """
        CREATE TABLE IF NOT EXISTS trades (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            broker TEXT NOT NULL,
            broker_trade_id TEXT,
            instrument TEXT NOT NULL,
            open_trade BOOLEAN NOT NULL DEFAULT 1,
            open_price REAL,
            close_price REAL,
            open_date TEXT,
            close_date TEXT
        );
        CREATE UNIQUE INDEX IF NOT EXISTS idx_one_open_per_instrument_per_broker
            ON trades(broker, instrument)
            WHERE open_trade = 1;
        """;

    public static void connect() {
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                System.out.println("Connected to TradeHistory database.");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        createDatabase();
    }

    private static void createDatabase() {
        try (Connection conn = DriverManager.getConnection(url)) {
            java.sql.Statement stmt = conn.createStatement();
            stmt.execute(createTradeHistoryTable);
            System.out.println("Table created.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static boolean checkForOpenTrade(String broker, String tradeId) {
        String query = """
                SELECT  open_trade
                FROM    trades
                WHERE   broker          = ?  AND
                        broker_trade_id = ?  AND
                        open_trade      = 1;
                """;

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement(query)) {
            // Set the query parameters
            pstmt.setString(1, broker);
            pstmt.setString(2, tradeId);

            // Executes the query, rs.next() will return true/false depending on if there is a row
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

}
