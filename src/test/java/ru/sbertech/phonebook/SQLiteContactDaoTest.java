package ru.sbertech.phonebook;

import org.junit.jupiter.api.*;
import ru.sbertech.phonebook.dao.SQLiteContactDao;
import ru.sbertech.phonebook.model.Employee;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты DAO-слоя (6 тестов).
 *
 * Стратегия тестирования:
 *   Одно соединение "jdbc:sqlite::memory:" открывается в @BeforeAll и живёт
 *   до конца класса. Это же соединение передаётся в SQLiteContactDao(Connection),
 *   поэтому все обращения к БД видят одни и те же таблицы и данные.
 *
 * Почему НЕ используется "jdbc:sqlite::memory:" с несколькими getConnection():
 *   Каждый вызов DriverManager.getConnection("jdbc:sqlite::memory:") открывает
 *   новую независимую пустую базу данных. Разделить её между setUp() и DAO
 *   без single-connection подхода невозможно без включения URI-режима.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SQLiteContactDaoTest {

    private Connection con;
    private SQLiteContactDao dao;

    @BeforeAll
    void setUp() throws Exception {
        // Одно соединение для всего класса — передаётся в DAO
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
            st.execute("INSERT INTO departments(name) VALUES ('Разработка')");
            st.execute("""
                INSERT INTO employees
                    (last_name, first_name, middle_name, position,
                     phone_work, phone_mobile, email, date_of_birth, department_id)
                VALUES
                    ('Иванов', 'Иван', 'Иванович', 'Разработчик',
                     '71234567890', '79001234567', 'ivanov@test.ru', '1990-05-15', 1)
                """);
        }

        // DAO получает то же самое соединение
        dao = new SQLiteContactDao(con);
    }

    @AfterAll
    void tearDown() throws Exception {
        if (con != null && !con.isClosed()) con.close();
    }

    // ── Тест 1: findAll() возвращает все записи ─────────────────
    @Test
    @DisplayName("findAll() возвращает непустой список, первая запись — Иванов")
    void findAll_returnsAllEmployees() {
        List<Employee> result = dao.findAll();
        assertFalse(result.isEmpty(), "findAll() не должен возвращать пустой список");
        assertEquals("Иванов", result.get(0).getLastName());
    }

    // ── Тест 2: findByLastName — совпадение есть ─────────────────
    @Test
    @DisplayName("findByLastName() с совпадением возвращает результат")
    void findByLastName_withMatch_returnsResult() {
        List<Employee> result = dao.findByLastName("Иван");
        assertFalse(result.isEmpty(),
            "Должен найти сотрудника с фамилией 'Иванов' по подстроке 'Иван'");
    }

    // ── Тест 3: findByLastName — совпадений нет ──────────────────
    @Test
    @DisplayName("findByLastName() без совпадений возвращает пустой список")
    void findByLastName_noMatch_returnsEmpty() {
        List<Employee> result = dao.findByLastName("НесуществующаяФамилияXYZ");
        assertTrue(result.isEmpty(),
            "Должен вернуть пустой список при отсутствии совпадений");
    }

    // ── Тест 4: insert → findAll содержит новую запись ───────────
    @Test
    @DisplayName("insert() добавляет сотрудника, findAll() его находит")
    void insert_thenFindAll_containsNewEmployee() {
        Employee emp = new Employee();
        emp.setLastName("Петров");
        emp.setFirstName("Пётр");
        emp.setPhoneMobile("79009999999");
        emp.setDepartmentId(1);

        dao.insert(emp);

        boolean found = dao.findAll().stream()
            .anyMatch(e -> "Петров".equals(e.getLastName()));
        assertTrue(found, "После вставки сотрудник должен присутствовать в findAll()");
    }

    // ── Тест 5: update изменяет поле ────────────────────────────
    @Test
    @DisplayName("update() сохраняет изменённое поле в БД")
    void update_changesField() {
        List<Employee> before = dao.findByLastName("Иванов");
        assertFalse(before.isEmpty(), "Иванов должен присутствовать в БД");

        Employee emp = before.get(0);
        emp.setPosition("Тест-обновление");
        dao.update(emp);

        List<Employee> after = dao.findByLastName("Иванов");
        assertEquals("Тест-обновление", after.get(0).getPosition(),
            "Поле position должно быть обновлено");
    }

    // ── Тест 6: delete удаляет запись ───────────────────────────
    @Test
    @DisplayName("delete() удаляет сотрудника из БД")
    void delete_removesEmployee() {
        Employee emp = new Employee();
        emp.setLastName("Временный");
        emp.setFirstName("Тест");
        emp.setPhoneMobile("79000000001");
        emp.setDepartmentId(1);
        dao.insert(emp);

        List<Employee> afterInsert = dao.findByLastName("Временный");
        assertFalse(afterInsert.isEmpty(), "Сотрудник должен быть добавлен");

        dao.delete(afterInsert.get(0).getId());

        List<Employee> afterDelete = dao.findByLastName("Временный");
        assertTrue(afterDelete.isEmpty(),
            "После удаления сотрудник не должен находиться в БД");
    }

    // ── Тест 7: регистронезависимый поиск — нижний регистр ──────
    @Test
    @DisplayName("searchEmployees() находит 'Иванов' по запросу 'иванов' (нижний регистр)")
    void searchEmployees_lowercaseQuery_findsEmployee() {
        List<Employee> results = dao.searchEmployees("иванов");
        assertFalse(results.isEmpty(),
            "Регистронезависимый поиск: 'иванов' должен найти 'Иванов'");
        assertTrue(results.stream().anyMatch(e -> "Иванов".equals(e.getLastName())),
            "В результатах должен быть сотрудник с фамилией 'Иванов'");
    }

    // ── Тест 8: регистронезависимый поиск — верхний регистр ─────
    @Test
    @DisplayName("searchEmployees() находит 'Иванов' по запросу 'ИВАНОВ' (верхний регистр)")
    void searchEmployees_uppercaseQuery_findsEmployee() {
        List<Employee> results = dao.searchEmployees("ИВАНОВ");
        assertFalse(results.isEmpty(),
            "Регистронезависимый поиск: 'ИВАНОВ' должен найти 'Иванов'");
        assertTrue(results.stream().anyMatch(e -> "Иванов".equals(e.getLastName())),
            "В результатах должен быть сотрудник с фамилией 'Иванов'");
    }
}
