# Телефонная адресная книга 
## ВКР Сомов А.С. | Java SE + Swing + SQLite + JDBC + Maven + JUnit 5

---

## Быстрый старт

### Требования
- JDK 17+
- Maven 3.6+

### Сборка и запуск
```bash
mvn clean package -q
java -jar target/phonebook.jar
```

### Запуск тестов
```bash
mvn test
```

---

## Структура проекта

```
phonebook/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/ru/sbertech/phonebook/
│   │   │   ├── Main.java                         # Точка входа
│   │   │   ├── model/
│   │   │   │   ├── Employee.java                 # Сотрудник
│   │   │   │   ├── Department.java               # Подразделение
│   │   │   │   └── AppUser.java                  # Пользователь системы
│   │   │   ├── dao/
│   │   │   │   ├── ContactDao.java               # Интерфейс (FR-1..FR-7)
│   │   │   │   ├── DepartmentDao.java            # Интерфейс
│   │   │   │   ├── AppUserDao.java               # Интерфейс (FR-8)
│   │   │   │   ├── SQLiteContactDao.java         # Реализация DAO сотрудников
│   │   │   │   ├── SQLiteDepartmentDao.java      # Реализация DAO подразделений
│   │   │   │   └── SQLiteAppUserDao.java         # Реализация DAO пользователей
│   │   │   ├── controller/
│   │   │   │   ├── AppController.java            # MVC-контроллер
│   │   │   │   └── ContactValidator.java         # Валидация полей
│   │   │   ├── view/
│   │   │   │   ├── MainFrame.java                # Главное окно (JFrame)
│   │   │   │   ├── EmployeeDialog.java           # Диалог добавления/редактирования
│   │   │   │   ├── LoginDialog.java              # Диалог аутентификации
│   │   │   │   └── EmployeeTableModel.java       # Модель данных таблицы
│   │   │   └── util/
│   │   │       ├── DbInitializer.java            # Инициализация БД
│   │   │       └── PasswordUtil.java             # SHA-256 хэширование
│   │   └── resources/
│   │       ├── schema.sql                        # DDL: CREATE TABLE (SQLite-синтаксис)
│   │       └── data.sql                          # Тестовые данные + администратор
│   └── test/java/ru/sbertech/phonebook/
│       ├── SQLiteContactDaoTest.java             # 6 тестов DAO
│       ├── ContactValidatorTest.java             # 9 тестов валидации
│       └── AppControllerTest.java               # 2 теста контроллера (login)
```

---

## Учётные данные администратора (по умолчанию)
| Логин | Пароль   |
|-------|---------|
| admin | admin123 |

Пароль хранится в БД в виде SHA-256 хэша.

---

## Требования, реализованные в проекте

| ID    | Описание                                    | Реализация                            |
|-------|---------------------------------------------|---------------------------------------|
| FR-1  | Отображение полного списка при запуске      | `ContactDao.findAll()`                |
| FR-2  | Поиск по частичному совпадению с фамилией  | `ContactDao.findByLastName()`         |
| FR-3  | Фильтрация по подразделению                 | `ContactDao.findByDepartment()`       |
| FR-4  | Поиск по номеру телефона                    | `ContactDao.findByPhone()`            |
| FR-5  | Добавление записи администратором           | `AppController.addEmployee()`         |
| FR-6  | Редактирование записи                       | `AppController.updateEmployee()`      |
| FR-7  | Удаление записи                             | `AppController.deleteEmployee()`      |
| FR-8  | Аутентификация администратора               | `AppController.login()` + SHA-256     |
| NFR-1 | Время отклика < 1 с на 1000 записей         | SQLite + индексы на поисковых полях   |
| NFR-2 | Целостность данных при сбое                 | Транзакционная модель JDBC            |
| NFR-3 | Эргономика: поиск в одном окне + валидация  | MainFrame + ContactValidator          |
| NFR-4 | Защита пароля + SQL-инъекции                | SHA-256 + PreparedStatement           |

---

## Архитектура MVC

```
View (Swing)          Controller (AppController)     DAO (SQLite)
─────────────         ──────────────────────         ────────────
MainFrame         →   findAll / findByLastName    →   SQLiteContactDao
LoginDialog       →   login / logout              →   SQLiteAppUserDao
EmployeeDialog    →   addEmployee / updateEmployee →   SQLiteContactDao
                      deleteEmployee              →   SQLiteContactDao
                      getDepartments              →   SQLiteDepartmentDao
```

---

## Стратегия SQLite in-memory в тестах

Стандартный URL `jdbc:sqlite::memory:` создаёт **новую изолированную БД** при каждом
вызове `DriverManager.getConnection()`. Поэтому таблицы, созданные в `@BeforeAll`,
не были бы видны внутри DAO, если бы DAO открывал своё соединение.

**Реализованное решение — одно разделяемое соединение:**

```java
// setUp(): открываем одно соединение
con = DriverManager.getConnection("jdbc:sqlite::memory:");

// Создаём схему и данные через это же соединение
con.createStatement().execute("CREATE TABLE ...");

// Передаём соединение в DAO — теперь DAO видит те же таблицы
dao = new SQLiteContactDao(con);   // конструктор Connection
```

Все три DAO-класса поддерживают два конструктора:
- `SQLiteContactDao(String dbUrl)` — для приложения (открывает соединение на каждый запрос);
- `SQLiteContactDao(Connection sharedCon)` — для тестов (работает через переданное соединение).
