package bot.data;

import java.sql.*;

public class Database {
    private static String url = "jdbc:sqlite:TradeHistory";

    private final static String createTradeHistoryTable = """
        CREATE TABLE IF NOT EXISTS trades (
            id                  INTEGER PRIMARY KEY AUTOINCREMENT,
            broker              TEXT NOT NULL,
            broker_trade_id     TEXT,
            instrument          TEXT NOT NULL,
            open_trade          BOOLEAN NOT NULL DEFAULT 1,
            open_price          REAL,
            close_price         REAL,
            open_date           TEXT,
            close_date          TEXT,
            trade_size          REAL
        );
        """;

    // Ensures there will only be one open trade at a time for a given broker and instrument
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
            System.out.println("trades table created");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Logs a new open trade
    public static boolean insertOpenTrade(String broker, String tradeId, String instrument, double openPrice, String openDate, double tradeSize) {
        String sql = """
                INSERT INTO trades(broker, broker_trade_id, instrument, open_trade, open_price, open_date, trade_size)
                            VALUES(?, ?, ?, 1, ?, ?, ?);
                """;

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Set the statement parameters
            pstmt.setString(1, broker);
            pstmt.setString(2, tradeId);
            pstmt.setString(3, instrument);
            pstmt.setDouble(4, openPrice);
            pstmt.setString(5, openDate);
            pstmt.setDouble(6, tradeSize);

            // Execute query
            pstmt.executeUpdate();
            System.out.println("trade inserted");
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    // Marks a trade with the provided tradeId and broker as closed
    public static boolean closeOpenTrade(String tradeId, String broker, double closePrice, String closeDate) {
        String sql = """
                UPDATE  trades
                SET     close_price     = ?,
                        close_date      = ?,
                        open_trade      = 0
                WHERE   broker_trade_id = ? AND
                        broker          = ? AND
                        open_trade      = 1
                """;

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Set the statement parameters
            pstmt.setDouble(1, closePrice);
            pstmt.setString(2, closeDate);
            pstmt.setString(3, tradeId);
            pstmt.setString(4, broker);

            // Execute query
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Trade closed");
                return true;
            } else {
                System.out.println("No open trades found");
                return false;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }

    }

    public static String getOpenTradeId(String broker, String instrument) {
        String sql = """
                SELECT  broker_trade_id
                FROM    trades
                WHERE   broker          = ?  AND
                        instrument      = ?  AND
                        open_trade      = 1;
                """;

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Set the statement parameters
            pstmt.setString(1, broker);
            pstmt.setString(2, instrument);

            // Execute the query and return the trade id, if found
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("broker_trade_id");
            } else {
                // Throw custom database exception?
                return null;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
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
                                ", open_date=" + rs.getString("open_date") +
                                ", close_price=" + rs.getDouble("close_price") +
                                ", close_date=" + rs.getString("close_date") +
                                ", close_date=" + rs.getDouble("trade_size")
                );
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

}
