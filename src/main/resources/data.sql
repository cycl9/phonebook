-- Справочные данные: подразделения и администратор.
-- Файл безопасен для повторного запуска: INSERT OR IGNORE не дублирует строки.
-- Начальные данные сотрудников загружаются из employees_seed.sql
-- только при первом запуске (когда таблица employees пуста).

INSERT OR IGNORE INTO departments(name) VALUES
    ('Разработка'),
    ('Аналитика'),
    ('Тестирование'),
    ('DevOps'),
    ('Управление');

-- Администратор: login=admin, password=admin123 (SHA-256)
INSERT OR IGNORE INTO app_users(username, password_hash) VALUES
    ('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9');
