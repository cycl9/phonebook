package ru.phonebook.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.stream.Collectors;

/**
 * Инициализирует базу данных SQLite.
 *
 * Порядок шагов при каждом запуске:
 *   1. PRAGMA foreign_keys = ON
 *   2. schema.sql   — CREATE TABLE/INDEX IF NOT EXISTS (всегда idempotent)
 *   3. Миграция M1  — однократно: нормализация + дедупликация + UNIQUE indexes
 *      Триггер: отсутствие индекса uidx_emp_phone_work в sqlite_master.
 *      При наличии индекса шаг полностью пропускается (O(1)).
 *   4. data.sql     — INSERT OR IGNORE для departments и app_users
 *   5. employees_seed.sql — plain INSERT, только если employees пуста
 *
 * Алгоритм дедупликации (M1):
 *   a. Нормализация phone_work, phone_mobile, email:
 *      blank/пробельная строка → NULL; trim; email → lower(trim)
 *   b. DELETE дублей, оставляя строку с наименьшим id:
 *      для каждого поля — DELETE WHERE id NOT IN (SELECT MIN(id) GROUP BY field)
 *   c. CREATE UNIQUE INDEX IF NOT EXISTS с условием WHERE field IS NOT NULL AND trim(field) <> ''
 *
 * Метод initialize() вызывается ровно один раз из Main.main().
 */
public class DbInitializer {

    private DbInitializer() {}

    public static void initialize(String dbUrl) {
        try (Connection con = DriverManager.getConnection(dbUrl)) {

            try (Statement st = con.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }

            executeSql(con, "/schema.sql");
            runMigrationM1(con);
            executeSql(con, "/data.sql");

            if (isTableEmpty(con, "employees")) {
                executeSql(con, "/employees_seed.sql");
            }

        } catch (SQLException | IOException e) {
            throw new RuntimeException("Ошибка инициализации БД: " + e.getMessage(), e);
        }
    }

    // ── Migration M1: normalize + dedup + UNIQUE indexes ─────────

    /**
     * Выполняется только один раз — когда uidx_emp_phone_work ещё не существует.
     * На чистой БД (первый запуск) таблица employees пуста → нормализация и
     * удаление дублей — no-op; индексы создаются сразу.
     * На загрязнённой БД (дубли от старого бага) — дубли удаляются корректно,
     * после чего индексы создаются без ошибок.
     */
    private static void runMigrationM1(Connection con) throws SQLException {
        if (indexExists(con, "uidx_emp_phone_work")) {
            return; // уже выполнялась
        }
        System.out.println("INFO DbInitializer: running migration M1 (deduplicate + create UNIQUE indexes)");
        deduplicateEmployees(con);
        createUniqueIndexes(con);
        System.out.println("INFO DbInitializer: migration M1 complete");
    }

    /**
     * Шаг 1 миграции M1: нормализация данных и удаление дублей в одной транзакции.
     *
     * Нормализация:
     *   phone_work, phone_mobile → trim(); blank → NULL
     *   email → lower(trim()); blank → NULL
     *
     * Дедупликация (для каждого уникального поля):
     *   Оставляем строку с MIN(id) — самую раннюю. Остальные удаляем.
     */
    private static void deduplicateEmployees(Connection con) throws SQLException {
        boolean prevAutoCommit = con.getAutoCommit();
        con.setAutoCommit(false);
        try (Statement st = con.createStatement()) {

            // ── Нормализация phone_work ──────────────────────────
            st.execute("UPDATE employees SET phone_work = NULL " +
                       "WHERE phone_work IS NOT NULL AND trim(phone_work) = ''");
            st.execute("UPDATE employees SET phone_work = trim(phone_work) " +
                       "WHERE phone_work IS NOT NULL AND phone_work != trim(phone_work)");

            // ── Нормализация phone_mobile ────────────────────────
            st.execute("UPDATE employees SET phone_mobile = NULL " +
                       "WHERE phone_mobile IS NOT NULL AND trim(phone_mobile) = ''");
            st.execute("UPDATE employees SET phone_mobile = trim(phone_mobile) " +
                       "WHERE phone_mobile IS NOT NULL AND phone_mobile != trim(phone_mobile)");

            // ── Нормализация email ───────────────────────────────
            st.execute("UPDATE employees SET email = NULL " +
                       "WHERE email IS NOT NULL AND trim(email) = ''");
            st.execute("UPDATE employees SET email = lower(trim(email)) " +
                       "WHERE email IS NOT NULL AND email != lower(trim(email))");

            // ── Дедупликация phone_work (keep MIN id) ────────────
            st.execute(
                "DELETE FROM employees " +
                "WHERE phone_work IS NOT NULL AND trim(phone_work) <> '' " +
                "  AND id NOT IN (" +
                "    SELECT MIN(id) FROM employees " +
                "    WHERE phone_work IS NOT NULL AND trim(phone_work) <> '' " +
                "    GROUP BY phone_work)");

            // ── Дедупликация phone_mobile (keep MIN id) ──────────
            st.execute(
                "DELETE FROM employees " +
                "WHERE phone_mobile IS NOT NULL AND trim(phone_mobile) <> '' " +
                "  AND id NOT IN (" +
                "    SELECT MIN(id) FROM employees " +
                "    WHERE phone_mobile IS NOT NULL AND trim(phone_mobile) <> '' " +
                "    GROUP BY phone_mobile)");

            // ── Дедупликация email (keep MIN id) ─────────────────
            st.execute(
                "DELETE FROM employees " +
                "WHERE email IS NOT NULL AND trim(email) <> '' " +
                "  AND id NOT IN (" +
                "    SELECT MIN(id) FROM employees " +
                "    WHERE email IS NOT NULL AND trim(email) <> '' " +
                "    GROUP BY email)");

            con.commit();
        } catch (SQLException e) {
            try { con.rollback(); } catch (SQLException ignored) {}
            throw e;
        } finally {
            con.setAutoCommit(prevAutoCommit);
        }
    }

    /** Шаг 2 миграции M1: создание UNIQUE partial indexes. */
    private static void createUniqueIndexes(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS uidx_emp_phone_work " +
                "ON employees(phone_work) " +
                "WHERE phone_work IS NOT NULL AND trim(phone_work) <> ''");
            st.execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS uidx_emp_phone_mobile " +
                "ON employees(phone_mobile) " +
                "WHERE phone_mobile IS NOT NULL AND trim(phone_mobile) <> ''");
            st.execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS uidx_emp_email " +
                "ON employees(email) " +
                "WHERE email IS NOT NULL AND trim(email) <> ''");
        }
    }

    // ── helpers ──────────────────────────────────────────────────

    private static boolean indexExists(Connection con, String name) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type='index' AND name=?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean isTableEmpty(Connection con, String table) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    /**
     * Читает SQL-файл из classpath и выполняет каждый оператор отдельно.
     * Строки-комментарии (начинающиеся с «--») пропускаются перед сплитом.
     */
    private static void executeSql(Connection con, String resourcePath)
            throws SQLException, IOException {

        try (InputStream in = DbInitializer.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Ресурс не найден в classpath: " + resourcePath);
            }
            String content = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines()
                    .filter(line -> !line.stripLeading().startsWith("--"))
                    .collect(Collectors.joining("\n"));

            for (String statement : content.split(";")) {
                String sql = statement.strip();
                if (!sql.isEmpty()) {
                    try (Statement st = con.createStatement()) {
                        st.execute(sql);
                    }
                }
            }
        }
    }
}
