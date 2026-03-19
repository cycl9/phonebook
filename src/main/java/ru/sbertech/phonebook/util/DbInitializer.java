package ru.sbertech.phonebook.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.stream.Collectors;

/**
 * Инициализирует базу данных SQLite: создаёт таблицы из schema.sql,
 * загружает справочные данные из data.sql (подразделения, пользователи)
 * и однократно заполняет таблицу employees из employees_seed.sql
 * (только если она пуста — т.е. при первом запуске).
 *
 * Стратегия seed для сотрудников:
 *   — data.sql использует INSERT OR IGNORE → безопасен для повторных запусков.
 *   — employees_seed.sql использует plain INSERT → выполняется только один раз
 *     при условии isEmpty(employees). Повторный запуск приложения не дублирует
 *     записи, потому что таблица уже не пуста.
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
            executeSql(con, "/data.sql");

            // Сотрудников seed только при первом запуске (пустая таблица)
            if (isTableEmpty(con, "employees")) {
                executeSql(con, "/employees_seed.sql");
            }

        } catch (SQLException | IOException e) {
            throw new RuntimeException("Ошибка инициализации БД: " + e.getMessage(), e);
        }
    }

    /**
     * Читает SQL-файл из classpath и выполняет каждое выражение отдельно.
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

    /** Возвращает true, если таблица не содержит ни одной строки. */
    private static boolean isTableEmpty(Connection con, String table) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }
}
