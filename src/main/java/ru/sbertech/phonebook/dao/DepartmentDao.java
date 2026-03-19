package ru.sbertech.phonebook.dao;

import ru.sbertech.phonebook.model.Department;
import java.util.List;

public interface DepartmentDao {
    List<Department> findAll();
}
