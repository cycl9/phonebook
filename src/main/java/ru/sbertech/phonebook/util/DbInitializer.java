package ru.sbertech.phonebook.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.stream.Collectors;

/**
 * Инициализирует базу данных SQLite: создаёт таблицы из schema.sql
 * и загружает тестовые данные из data.sql.
 *
 * Оба SQL-файла — единственный источник истины о схеме.
 * DbInitializer только читает их и выполняет; дублирования DDL нет.
 *
 * Каждое SQL-выражение разделяется символом ';' с обрезкой пробелов.
 * Комментарии вида «--» пропускаются при разборе.
 */
public class DbInitializer {

    private DbInitializer() {}

    public static void initialize(String dbUrl) {
        try (Connection con = DriverManager.getConnection(dbUrl);
             Statement  st  = con.createStatement()) {

            st.execute("PRAGMA foreign_keys = ON");

            executeSql(con, "/schema.sql");
            executeSql(con, "/data.sql");

        } catch (SQLException | IOException e) {
            throw new RuntimeException("Ошибка инициализации БД: " + e.getMessage(), e);
        }
    }

    /**
     * Читает SQL-файл из classpath и выполняет каждое выражение отдельно.
     * Строки-комментарии (начинающиеся с «--») пропускаются.
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

            // Разбиваем по «;» и выполняем каждый непустой оператор
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
