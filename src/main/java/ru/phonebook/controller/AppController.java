package ru.phonebook.controller;

import ru.phonebook.dao.AppUserDao;
import ru.phonebook.dao.ContactDao;
import ru.phonebook.dao.DepartmentDao;
import ru.phonebook.model.AppUser;
import ru.phonebook.model.Department;
import ru.phonebook.model.Employee;
import ru.phonebook.util.PasswordUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AppController {

    private final ContactDao    contactDao;
    private final DepartmentDao departmentDao;
    private final AppUserDao    userDAO;
    private final ContactValidator validator;
    private boolean adminSession = false;

    public AppController(ContactDao contactDao, DepartmentDao departmentDao, AppUserDao userDAO) {
        this.contactDao = contactDao; this.departmentDao = departmentDao;
        this.userDAO = userDAO; this.validator = new ContactValidator();
    }

    public boolean login(String username, String password) {
        Optional<AppUser> user = userDAO.findByUsername(username);
        if (user.isPresent() &&
                PasswordUtil.verify(password, user.get().getPasswordHash())) {
            adminSession = true; return true;
        }
        return false;
    }
    public void logout()            { adminSession = false; }
    public boolean isAdminSession() { return adminSession; }

    public List<Employee>   findAll()                  { return contactDao.findAll(); }
    public List<Employee>   findByLastName(String v)   { return contactDao.findByLastName(v); }
    public List<Employee>   findByFirstName(String v)  { return contactDao.findByFirstName(v); }
    public List<Employee>   findByName(String v)       { return contactDao.findByName(v); }
    public List<Employee>   searchEmployees(String v)  { return contactDao.searchEmployees(v); }

    /**
     * Комбинированный поиск: фильтр по подразделению + текстовый поиск.
     */
    public List<Employee> searchEmployees(String query, String dept) {
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasDept  = dept  != null && !dept.isBlank();
        if (!hasQuery && !hasDept) return findAll();
        if (!hasQuery) return contactDao.findByDepartment(dept);
        if (!hasDept)  return contactDao.searchEmployees(query);
        List<Employee> byDept = contactDao.findByDepartment(dept);
        String q = query.toLowerCase();
        return byDept.stream().filter(e ->
            matches(e.getLastName(),   q) || matches(e.getFirstName(),  q) ||
            matches(e.getMiddleName(), q) || matches(e.getPosition(),   q) ||
            matches(e.getPhoneWork(),  q) || matches(e.getPhoneMobile(), q)
        ).toList();
    }
    private static boolean matches(String field, String q) {
        return field != null && field.toLowerCase().contains(q);
    }

    public List<Employee>   findByDepartment(String v) { return contactDao.findByDepartment(v); }
    public List<Employee>   findByPhone(String v)      { return contactDao.findByPhone(v); }
    public List<Department> getDepartments()           { return departmentDao.findAll(); }

    /**
     * Добавляет сотрудника. Возвращает список ошибок (пустой — успех).
     * Нарушение UNIQUE constraint перехватывается и возвращается как
     * понятное сообщение — исключение не достигает UI.
     */
    public List<String> addEmployee(Employee emp) {
        requireAdmin();
        List<String> errors = validator.validate(emp);
        if (!errors.isEmpty()) return errors;
        try {
            contactDao.insert(emp);
            return List.of();
        } catch (RuntimeException ex) {
            return List.of(translateDaoError(ex));
        }
    }

    /**
     * Обновляет сотрудника. Возвращает список ошибок (пустой — успех).
     * SQLite корректно разрешает UPDATE строки с теми же значениями уникальных полей
     * (обновляемая строка не конфликтует сама с собой).
     */
    public List<String> updateEmployee(Employee emp) {
        requireAdmin();
        List<String> errors = validator.validate(emp);
        if (!errors.isEmpty()) return errors;
        try {
            contactDao.update(emp);
            return List.of();
        } catch (RuntimeException ex) {
            return List.of(translateDaoError(ex));
        }
    }

    public void deleteEmployee(int id) { requireAdmin(); contactDao.delete(id); }

    private void requireAdmin() {
        if (!adminSession) throw new SecurityException("Операция доступна только администратору.");
    }

    /**
     * Переводит исключение DAO в читаемое сообщение для UI.
     * Инспектирует getCause() (оригинальный SQLException) для точной локализации поля.
     */
    private static String translateDaoError(RuntimeException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof SQLException sqle) {
            String msg = sqle.getMessage() != null ? sqle.getMessage().toLowerCase() : "";
            // SQLite выбрасывает код 19 (SQLITE_CONSTRAINT) и сообщение вида
            // "UNIQUE constraint failed: employees.<column>"
            if (msg.contains("unique") || sqle.getErrorCode() == 19) {
                if (msg.contains("phone_work"))   return "Рабочий телефон уже зарегистрирован у другого сотрудника.";
                if (msg.contains("phone_mobile")) return "Мобильный телефон уже зарегистрирован у другого сотрудника.";
                if (msg.contains("email"))        return "Email уже зарегистрирован у другого сотрудника.";
                return "Нарушение уникальности данных. Проверьте телефоны и email.";
            }
            if (msg.contains("foreign key")) return "Указанное подразделение не существует.";
        }
        return "Ошибка сохранения: " + ex.getMessage();
    }
}
