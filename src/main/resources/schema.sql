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

CREATE INDEX IF NOT EXISTS idx_emp_last_name    ON employees(last_name);
CREATE INDEX IF NOT EXISTS idx_emp_phone_work   ON employees(phone_work);
CREATE INDEX IF NOT EXISTS idx_emp_phone_mobile ON employees(phone_mobile);
