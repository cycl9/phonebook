-- ============================================================
-- Схема БД: Телефонная адресная книга (SQLite-синтаксис)
-- ============================================================

CREATE TABLE IF NOT EXISTS departments (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE
);

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
    CONSTRAINT fk_dept FOREIGN KEY (department_id)
        REFERENCES departments(id)
);

CREATE TABLE IF NOT EXISTS app_users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL
);

-- Обычный индекс для поиска по фамилии
CREATE INDEX IF NOT EXISTS idx_emp_last_name ON employees(last_name);

-- UNIQUE partial indexes: уникальность только среди непустых значений.
-- NULL и пустая строка после trim() из проверки исключены —
-- несколько сотрудников могут не иметь рабочего или мобильного телефона.
CREATE UNIQUE INDEX IF NOT EXISTS uidx_emp_phone_work
    ON employees(phone_work)
    WHERE phone_work IS NOT NULL AND trim(phone_work) <> '';

CREATE UNIQUE INDEX IF NOT EXISTS uidx_emp_phone_mobile
    ON employees(phone_mobile)
    WHERE phone_mobile IS NOT NULL AND trim(phone_mobile) <> '';

CREATE UNIQUE INDEX IF NOT EXISTS uidx_emp_email
    ON employees(email)
    WHERE email IS NOT NULL AND trim(email) <> '';
