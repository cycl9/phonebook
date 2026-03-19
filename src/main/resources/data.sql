-- Тестовые данные
INSERT OR IGNORE INTO departments(name) VALUES
    ('Разработка'),
    ('Аналитика'),
    ('Тестирование'),
    ('DevOps'),
    ('Управление');

INSERT OR IGNORE INTO employees
    (last_name, first_name, middle_name, position, phone_work, phone_mobile, email, date_of_birth, department_id)
VALUES
    ('Иванов',    'Иван',    'Иванович',   'Ведущий разработчик', '74991112233', '79161112233', 'ivanov@example.com',   '1990-05-15', 1),
    ('Петрова',   'Елена',   'Сергеевна',  'Бизнес-аналитик',     '74991112244', '79162223344', 'petrova@example.com',  '1988-11-20', 2),
    ('Сидоров',   'Алексей', 'Петрович',   'QA-инженер',          '74991112255', '79163334455', 'sidorov@example.com',  '1993-03-07', 3),
    ('Козлова',   'Мария',   'Андреевна',  'DevOps-инженер',      '74991112266', '79164445566', 'kozlova@example.com',  '1991-07-25', 4),
    ('Новиков',   'Дмитрий', 'Олегович',   'Менеджер проекта',    '74991112277', '79165556677', 'novikov@example.com',  '1985-12-01', 5),
    ('Морозова',  'Анна',    'Викторовна', 'Java-разработчик',    '74991112288', '79166667788', 'morozova@example.com', '1994-09-18', 1),
    ('Волков',    'Сергей',  'Николаевич', 'Системный аналитик',  '74991112299', '79167778899', 'volkov@example.com',   '1987-04-30', 2),
    ('Лебедева',  'Ольга',   'Ивановна',   'Тест-лид',            '74991112300', '79168889900', 'lebedeva@example.com', '1992-08-14', 3);

-- Администратор: login=admin, password=admin123 (SHA-256)
INSERT OR IGNORE INTO app_users(username, password_hash) VALUES
    ('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9');
