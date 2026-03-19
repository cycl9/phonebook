package ru.sbertech.phonebook.dao;

import ru.sbertech.phonebook.model.Employee;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite-реализация ContactDao.
 *
 * Два конструктора:
 *   SQLiteContactDao(String dbUrl)      — приложение, каждый запрос открывает своё соединение
 *   SQLiteContactDao(Connection shared) — тесты, используется одно переданное соединение
 *
 * Нормализация данных перед записью в БД (метод setParams):
 *   — trim() для всех строковых полей
 *   — blank / пустая строка → NULL (чтобы UNIQUE partial indexes корректно
 *     исключали «отсутствующие» значения из проверки уникальности)
 *   — email дополнительно приводится к нижнему регистру
 *
 * Для всех соединений при открытии выполняется PRAGMA foreign_keys = ON.
 * sharedCon никогда не закрывается внутри DAO.
 */
public class SQLiteContactDao implements ContactDao {

    private final String     dbUrl;
    private final Connection sharedCon;

    public SQLiteContactDao(String dbUrl)         { this.dbUrl = dbUrl; this.sharedCon = null; }
    public SQLiteContactDao(Connection sharedCon) { this.dbUrl = null;  this.sharedCon = sharedCon; }

    private static final String BASE =
        "SELECT e.id, e.last_name, e.first_name, e.middle_name, " +
        "e.position, e.phone_work, e.phone_mobile, e.email, " +
        "e.date_of_birth, e.department_id, d.name AS dept " +
        "FROM employees e JOIN departments d ON e.department_id = d.id ";

    // ── FR-1 ─────────────────────────────────────────────────────
    @Override
    public List<Employee> findAll() {
        return exec(BASE + "ORDER BY e.last_name, e.first_name");
    }

    // ── FR-2 + FR-4: универсальный поиск ─────────────────────────
    @Override
    public List<Employee> searchEmployees(String query) {
        if (query == null || query.isBlank()) return findAll();
        if (query.length() > 200) query = query.substring(0, 200);
        String[] parts = query.trim().split("\\s+");
        if (parts.length > 5) parts = java.util.Arrays.copyOf(parts, 5);

        if (parts.length >= 3) {
            String p1 = "%" + parts[0] + "%", p2 = "%" + parts[1] + "%", p3 = "%" + parts[2] + "%";
            return execQuery(BASE +
                "WHERE (e.last_name LIKE ? AND e.first_name LIKE ? AND e.middle_name LIKE ?) " +
                "   OR (e.last_name LIKE ? AND e.first_name LIKE ?) " +
                "   OR (e.last_name LIKE ? AND e.first_name LIKE ?) " +
                "ORDER BY e.last_name, e.first_name",
                ps -> {
                    ps.setString(1, p1); ps.setString(2, p2); ps.setString(3, p3);
                    ps.setString(4, p1); ps.setString(5, p2);
                    ps.setString(6, p2); ps.setString(7, p1);
                });
        } else if (parts.length == 2) {
            String p1 = "%" + parts[0] + "%", p2 = "%" + parts[1] + "%";
            return execQuery(BASE +
                "WHERE (e.last_name LIKE ? AND e.first_name LIKE ?) " +
                "   OR (e.last_name LIKE ? AND e.first_name LIKE ?) " +
                "ORDER BY e.last_name, e.first_name",
                ps -> {
                    ps.setString(1, p1); ps.setString(2, p2);
                    ps.setString(3, p2); ps.setString(4, p1);
                });
        } else {
            String p = "%" + parts[0] + "%";
            return execQuery(BASE +
                "WHERE e.last_name LIKE ? OR e.first_name LIKE ? OR e.middle_name LIKE ? " +
                "   OR e.phone_work LIKE ? OR e.phone_mobile LIKE ? " +
                "ORDER BY e.last_name, e.first_name",
                ps -> {
                    ps.setString(1, p); ps.setString(2, p); ps.setString(3, p);
                    ps.setString(4, p); ps.setString(5, p);
                });
        }
    }

    @Override
    public List<Employee> findByLastName(String v) {
        return execParam(BASE + "WHERE e.last_name LIKE ? ORDER BY e.last_name", v);
    }

    @Override
    public List<Employee> findByFirstName(String v) {
        return execParam(BASE + "WHERE e.first_name LIKE ? ORDER BY e.last_name", v);
    }

    @Override
    public List<Employee> findByDepartment(String v) {
        return execParam(BASE + "WHERE d.name LIKE ? ORDER BY e.last_name", v);
    }

    @Override
    public List<Employee> findByPhone(String phone) {
        String p = "%" + phone + "%";
        return execQuery(BASE + "WHERE e.phone_work LIKE ? OR e.phone_mobile LIKE ? ORDER BY e.last_name",
            ps -> { ps.setString(1, p); ps.setString(2, p); });
    }

    @Override public List<Employee> findByName(String q) { return searchEmployees(q); }

    /**
     * Добавляет нового сотрудника.
     * Данные нормализуются в setParams перед записью.
     * При нарушении UNIQUE partial index бросает RuntimeException с причиной SQLException,
     * которую AppController перехватывает и переводит в понятное сообщение для UI.
     */
    @Override
    public void insert(Employee emp) {
        String sql =
            "INSERT INTO employees " +
            "  (last_name, first_name, middle_name, position, phone_work, " +
            "   phone_mobile, email, date_of_birth, department_id) " +
            "VALUES (?,?,?,?,?,?,?,?,?)";
        execUpdate(sql, ps -> setParams(ps, emp));
    }

    /** Обновляет сотрудника. SQLite корректно обрабатывает UPDATE собственной строки
     *  — строка, которая обновляется, не конфликтует сама с собой в UNIQUE index. */
    @Override
    public void update(Employee emp) {
        String sql =
            "UPDATE employees SET last_name=?, first_name=?, middle_name=?, " +
            "position=?, phone_work=?, phone_mobile=?, email=?, " +
            "date_of_birth=?, department_id=? WHERE id=?";
        execUpdate(sql, ps -> { setParams(ps, emp); ps.setInt(10, emp.getId()); });
    }

    /** Удаляет сотрудника по id. */
    @Override
    public void delete(int id) {
        execUpdate("DELETE FROM employees WHERE id=?", ps -> ps.setInt(1, id));
    }

    // ── DML шаблон ───────────────────────────────────────────────

    @FunctionalInterface
    private interface PsSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private void execUpdate(String sql, PsSetter setter) {
        if (sharedCon != null) {
            try (PreparedStatement ps = sharedCon.prepareStatement(sql)) {
                setter.set(ps);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Ошибка записи (shared)", e);
            }
        } else {
            try (Connection con = DriverManager.getConnection(dbUrl)) {
                try (Statement pragma = con.createStatement()) {
                    pragma.execute("PRAGMA foreign_keys = ON");
                }
                con.setAutoCommit(false);
                try {
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        setter.set(ps);
                        ps.executeUpdate();
                    }
                    con.commit();
                } catch (SQLException e) {
                    try { con.rollback(); } catch (SQLException ignored) {}
                    throw new RuntimeException("Ошибка записи, транзакция отменена: " + e.getMessage(), e);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Ошибка соединения с БД: " + e.getMessage(), e);
            }
        }
    }

    // ── SELECT шаблоны ───────────────────────────────────────────

    @FunctionalInterface
    private interface PsQuery {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<Employee> execQuery(String sql, PsQuery setter) {
        Connection con = null;
        try {
            con = open();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                setter.set(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка запроса", e);
        } finally {
            if (con != null) try { closeIfOwn(con); } catch (SQLException ignored) {}
        }
    }

    private List<Employee> exec(String sql) {
        Connection con = null;
        try {
            con = open();
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка запроса", e);
        } finally {
            if (con != null) try { closeIfOwn(con); } catch (SQLException ignored) {}
        }
    }

    private List<Employee> execParam(String sql, String value) {
        return execQuery(sql, ps -> ps.setString(1, "%" + value + "%"));
    }

    // ── Connection helpers ────────────────────────────────────────

    private Connection open() throws SQLException {
        if (sharedCon != null) return sharedCon;
        Connection con = DriverManager.getConnection(dbUrl);
        try (Statement st = con.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return con;
    }

    private void closeIfOwn(Connection con) throws SQLException {
        if (sharedCon == null) con.close();
    }

    // ── Нормализация и маппинг ────────────────────────────────────

    /**
     * Устанавливает параметры PreparedStatement с нормализацией:
     *   - trim() для всех строковых полей
     *   - blank / пустая строка → NULL (совместимо с UNIQUE partial indexes)
     *   - email → lower(trim(email))
     */
    private void setParams(PreparedStatement ps, Employee emp) throws SQLException {
        ps.setString(1, trimRequired(emp.getLastName()));
        ps.setString(2, trimRequired(emp.getFirstName()));
        setNullable(ps, 3, normalize(emp.getMiddleName()));
        setNullable(ps, 4, normalize(emp.getPosition()));
        setNullable(ps, 5, normalize(emp.getPhoneWork()));
        setNullable(ps, 6, normalize(emp.getPhoneMobile()));
        setNullable(ps, 7, normalizeEmail(emp.getEmail()));
        if (emp.getDateOfBirth() != null) {
            ps.setString(8, emp.getDateOfBirth().toString());
        } else {
            ps.setNull(8, Types.VARCHAR);
        }
        ps.setInt(9, emp.getDepartmentId());
    }

    /** Для обязательных полей (last_name, first_name): только trim, null не возвращается. */
    private static String trimRequired(String s) {
        return s != null ? s.trim() : "";
    }

    /** Для необязательных полей: blank/null → null (исключается из UNIQUE partial index). */
    private static String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Email: trim + toLower + blank → null. */
    private static String normalizeEmail(String s) {
        return (s == null || s.isBlank()) ? null : s.trim().toLowerCase();
    }

    private static void setNullable(PreparedStatement ps, int i, String v) throws SQLException {
        if (v == null) ps.setNull(i, Types.VARCHAR);
        else           ps.setString(i, v);
    }

    private List<Employee> mapResultSet(ResultSet rs) throws SQLException {
        List<Employee> list = new ArrayList<>();
        while (rs.next()) {
            Employee e = new Employee();
            e.setId(rs.getInt("id"));
            e.setLastName(rs.getString("last_name"));
            e.setFirstName(rs.getString("first_name"));
            e.setMiddleName(rs.getString("middle_name"));
            e.setPosition(rs.getString("position"));
            e.setPhoneWork(rs.getString("phone_work"));
            e.setPhoneMobile(rs.getString("phone_mobile"));
            e.setEmail(rs.getString("email"));
            String dob = rs.getString("date_of_birth");
            if (dob != null) e.setDateOfBirth(LocalDate.parse(dob));
            e.setDepartmentId(rs.getInt("department_id"));
            e.setDepartmentName(rs.getString("dept"));
            list.add(e);
        }
        return list;
    }
}
