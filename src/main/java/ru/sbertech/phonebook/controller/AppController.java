package ru.sbertech.phonebook.controller;

import ru.sbertech.phonebook.dao.AppUserDao;
import ru.sbertech.phonebook.dao.ContactDao;
import ru.sbertech.phonebook.dao.DepartmentDao;
import ru.sbertech.phonebook.model.AppUser;
import ru.sbertech.phonebook.model.Department;
import ru.sbertech.phonebook.model.Employee;
import ru.sbertech.phonebook.util.PasswordUtil;

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
                user.get().getPasswordHash().equals(PasswordUtil.sha256hex(password))) {
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
    /** Универсальный поиск: одно или два слова, имя+фамилия, телефон */
    public List<Employee>   searchEmployees(String v)  { return contactDao.searchEmployees(v); }
    /**
     * Комбинированный поиск: сначала фильтрует по подразделению,
     * затем в рамках результата — по тексту через searchEmployees.
     * Вся логика поиска сосредоточена в контроллере; UI только передаёт параметры.
     */
    public List<Employee> searchEmployees(String query, String dept) {
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasDept  = dept  != null && !dept.isBlank();
        if (!hasQuery && !hasDept) return findAll();
        if (!hasQuery) return contactDao.findByDepartment(dept);
        if (!hasDept)  return contactDao.searchEmployees(query);
        // Оба условия: ищем по тексту внутри выборки по подразделению
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

    public List<String> addEmployee(Employee emp) {
        requireAdmin();
        List<String> errors = validator.validate(emp);
        if (errors.isEmpty()) contactDao.insert(emp);
        return errors;
    }
    public List<String> updateEmployee(Employee emp) {
        requireAdmin();
        List<String> errors = validator.validate(emp);
        if (errors.isEmpty()) contactDao.update(emp);
        return errors;
    }
    public void deleteEmployee(int id) { requireAdmin(); contactDao.delete(id); }

    private void requireAdmin() {
        if (!adminSession) throw new SecurityException("Операция доступна только администратору.");
    }
}
