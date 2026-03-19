package ru.sbertech.phonebook.dao;

import ru.sbertech.phonebook.model.Employee;
import java.util.List;

public interface ContactDao {
    List<Employee> findAll();

    /**
     * FR-2 / FR-4: универсальный поиск.
     * Ищет по first_name LIKE ? OR last_name LIKE ? OR phone_mobile LIKE ? OR phone_work LIKE ?
     * Один метод вместо трёх отдельных — пользователь вводит что угодно.
     */
    List<Employee> searchEmployees(String query);

    /** Оставляем для обратной совместимости с тестами */
    List<Employee> findByLastName(String lastName);
    List<Employee> findByFirstName(String firstName);
    List<Employee> findByName(String query);
    List<Employee> findByDepartment(String departmentName);
    List<Employee> findByPhone(String phone);

    void insert(Employee emp);
    void update(Employee emp);
    void delete(int id);
}
