package bot.data;

import javax.xml.transform.Result;
import java.sql.*;

public class Database {
    private static String url = "jdbc:sqlite:TradeHistory";

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
        """;

    private final static String createUniqueIndex = """
            CREATE UNIQUE INDEX IF NOT EXISTS idx_one_open_per_instrument_per_broker
                ON trades(broker, instrument)
                WHERE open_trade = 1;
            """;

    // Sets the URL to memory for test contexts
    public static void connectTestContext(String testUrl) {
        url = testUrl;
        connect();
    }

    public static void connect() {
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                System.out.println("Connected to database.");
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
            stmt.execute(createUniqueIndex);
            System.out.println("Table created.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static boolean insertOpenTrade(String broker, String tradeId, String instrument, double openPrice, String openDate) {
        String sql = """
                INSERT INTO trades(broker, broker_trade_id, instrument, open_trade, open_price, open_date)
                            VALUES(?, ?, ?, 1, ?, ?);
                """;

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Set the statement parameters
            pstmt.setString(1, broker);
            pstmt.setString(2, tradeId);
            pstmt.setString(3, instrument);
            pstmt.setDouble(4, openPrice);
            pstmt.setString(5, openDate);

            // Execute query
            pstmt.executeUpdate();
            System.out.println("trade inserted");
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static void closeOpenTrade(String tradeId, double closePrice, double closeDate) {

    }

    // Checks if there is an open trade for the provided broker and instrument
    public static boolean isOpenTrade(String broker, String instrument) {
        String sql = """
                SELECT  open_trade
                FROM    trades
                WHERE   broker          = ?  AND
                        instrument      = ?  AND
                        open_trade      = 1;
                """;

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Set the statement parameters
            pstmt.setString(1, broker);
            pstmt.setString(2, instrument);

            // Executes the query, rs.next() will return true/false depending on if there is a row
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static void printAllRows() {
        String sql = """
                SELECT  *
                FROM    trades
                """;

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Executes the query and prints each row
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println(
                        "id=" + rs.getInt("id") +
                                ", broker=" + rs.getString("broker") +
                                ", broker_trade_id=" + rs.getString("broker_trade_id") +
                                ", instrument=" + rs.getString("instrument") +
                                ", open_trade=" + rs.getBoolean("open_trade") +
                                ", open_price=" + rs.getDouble("open_price") +
                                ", open_date=" + rs.getDouble("open_price") +
                                ", close_price=" + rs.getDouble("open_price") +
                                ", close_date=" + rs.getDouble("open_price")
                );
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

}
