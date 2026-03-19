package ru.sbertech.phonebook.dao;

import ru.sbertech.phonebook.model.Department;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite-реализация DepartmentDao.
 * sharedCon НЕ закрывается — управление через finally, не через try-with-resources.
 */
public class SQLiteDepartmentDao implements DepartmentDao {

    private final String     dbUrl;
    private final Connection sharedCon;

    public SQLiteDepartmentDao(String dbUrl)         { this.dbUrl = dbUrl; this.sharedCon = null; }
    public SQLiteDepartmentDao(Connection sharedCon) { this.dbUrl = null;  this.sharedCon = sharedCon; }

    @Override
    public List<Department> findAll() {
        String sql = "SELECT id, name FROM departments ORDER BY name";
        Connection con = null;
        try {
            con = open();
            try (Statement st  = con.createStatement();
                 ResultSet rs  = st.executeQuery(sql)) {
                List<Department> list = new ArrayList<>();
                while (rs.next()) list.add(new Department(rs.getInt("id"), rs.getString("name")));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка загрузки подразделений", e);
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
