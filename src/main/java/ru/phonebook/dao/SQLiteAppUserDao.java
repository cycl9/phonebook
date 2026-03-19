package ru.phonebook.dao;

import ru.phonebook.model.AppUser;
import java.sql.*;
import java.util.Optional;

/**
 * SQLite-реализация AppUserDao.
 *
 * Два конструктора:
 *   SQLiteAppUserDao(String dbUrl)      — приложение
 *   SQLiteAppUserDao(Connection shared) — тесты (sharedCon НЕ закрывается DAO)
 *
 * Все операции с Connection выполняются через finally, а не через try-with-resources,
 * чтобы избежать автоматического close() над shared connection.
 */
public class SQLiteAppUserDao implements AppUserDao {

    private final String     dbUrl;
    private final Connection sharedCon;

    public SQLiteAppUserDao(String dbUrl)         { this.dbUrl = dbUrl; this.sharedCon = null; }
    public SQLiteAppUserDao(Connection sharedCon) { this.dbUrl = null;  this.sharedCon = sharedCon; }

    @Override
    public Optional<AppUser> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash FROM app_users WHERE username=?";
        Connection con = null;
        try {
            con = open();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new AppUser(
                                rs.getInt("id"),
                                rs.getString("username"),
                                rs.getString("password_hash")));
                    }
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска пользователя", e);
        } finally {
            if (con != null) try { closeIfOwn(con); } catch (SQLException ignored) {}
        }
    }

    private Connection open() throws SQLException {
        if (sharedCon != null) return sharedCon;
        Connection con = DriverManager.getConnection(dbUrl);
        try (Statement st = con.createStatement()) { st.execute("PRAGMA foreign_keys = ON"); }
        return con;
    }

    private void closeIfOwn(Connection con) throws SQLException {
        if (sharedCon == null) con.close();
    }
}
