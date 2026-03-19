package ru.sbertech.phonebook;

import org.junit.jupiter.api.*;
import ru.sbertech.phonebook.controller.AppController;
import ru.sbertech.phonebook.dao.*;
import ru.sbertech.phonebook.model.Employee;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты AppController (тесты 7–9).
 *
 * Одно соединение "jdbc:sqlite::memory:" открывается в @BeforeAll
 * и передаётся во все три DAO — они видят одну и ту же in-memory БД.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppControllerTest {

    private Connection con;
    private AppController controller;

    @BeforeAll
    void setUp() throws Exception {
        con = DriverManager.getConnection("jdbc:sqlite::memory:");

        try (Statement st = con.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("""
                CREATE TABLE IF NOT EXISTS departments (
                    id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    name VARCHAR(255) NOT NULL UNIQUE
                )""");
            st.execute("""
                CREATE TABLE IF NOT EXISTS employees (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    last_name      VARCHAR(100) NOT NULL,
                    first_name     VARCHAR(100) NOT NULL,
                    middle_name    VARCHAR(100),
                    position       VARCHAR(200),
                    phone_work     VARCHAR(30),
                    phone_mobile   VARCHAR(30),
                    email          VARCHAR(200),
                    date_of_birth  DATE,
                    department_id  INTEGER NOT NULL,
                    FOREIGN KEY (department_id) REFERENCES departments(id)
                )""");
            st.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uidx_emp_phone_work
                    ON employees(phone_work)
                    WHERE phone_work IS NOT NULL AND trim(phone_work) <> ''""");
            st.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uidx_emp_phone_mobile
                    ON employees(phone_mobile)
                    WHERE phone_mobile IS NOT NULL AND trim(phone_mobile) <> ''""");
            st.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uidx_emp_email
                    ON employees(email)
                    WHERE email IS NOT NULL AND trim(email) <> ''""");
            st.execute("""
                CREATE TABLE IF NOT EXISTS app_users (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      VARCHAR(100) NOT NULL UNIQUE,
                    password_hash VARCHAR(64)  NOT NULL
                )""");
            st.execute("INSERT INTO departments(name) VALUES ('Разработка')");
            // admin / admin123 → SHA-256
            st.execute("""
                INSERT INTO app_users(username, password_hash)
                VALUES ('admin',
                        '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9')
                """);
        }

        controller = new AppController(
            new SQLiteContactDao(con),
            new SQLiteDepartmentDao(con),
            new SQLiteAppUserDao(con)
        );
    }

    @AfterAll
    void tearDown() throws Exception {
        if (con != null && !con.isClosed()) con.close();
    }

    // ── Тест 7: успешный вход ────────────────────────────────────
    @Test
    @DisplayName("login() с верными данными устанавливает adminSession")
    void login_withCorrectCredentials_setsAdminSession() {
        assertTrue(controller.login("admin", "admin123"),
            "login() должен вернуть true при верных учётных данных");
        assertTrue(controller.isAdminSession(),
            "После успешного входа adminSession должен быть true");
        controller.logout();
        assertFalse(controller.isAdminSession(),
            "После logout adminSession должен быть false");
    }

    // ── Тест 8: неверный пароль ──────────────────────────────────
    @Test
    @DisplayName("login() с неверным паролем возвращает false")
    void login_withWrongPassword_returnsFalse() {
        assertFalse(controller.login("admin", "wrongpassword"),
            "login() должен вернуть false при неверном пароле");
        assertFalse(controller.isAdminSession(),
            "adminSession не должен быть установлен при неверном пароле");
    }

    // ── Тест 9: SecurityException без admin-сессии ───────────────
    @Test
    @DisplayName("addEmployee() без входа бросает SecurityException")
    void addEmployee_withoutAdminSession_throwsSecurityException() {
        controller.logout();
        Employee emp = new Employee();
        emp.setLastName("Тест");
        emp.setFirstName("Тест");
        emp.setPhoneMobile("79001234567");
        emp.setDepartmentId(1);
        assertThrows(SecurityException.class,
            () -> controller.addEmployee(emp),
            "addEmployee() без admin-сессии должен бросать SecurityException");
    }
}
