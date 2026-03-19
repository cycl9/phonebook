package ru.phonebook.dao;

import ru.phonebook.model.Department;
import java.util.List;

public interface DepartmentDao {
    List<Department> findAll();
}
