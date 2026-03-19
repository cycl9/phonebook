package ru.sbertech.phonebook.view;

import ru.sbertech.phonebook.model.Employee;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель таблицы для JTable — обновляется без прямого обращения к БД.
 */
public class EmployeeTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
        "ID", "Фамилия", "Имя", "Отчество", "Должность",
        "Раб.тел.", "Моб.тел.", "Email", "Подразделение"
    };

    private List<Employee> rows = new ArrayList<>();

    public void setData(List<Employee> data) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setData(data));
            return;
        }
        this.rows = new ArrayList<>(data);
        fireTableDataChanged();
    }

    public Employee getEmployeeAt(int row) { return rows.get(row); }

    @Override public int getRowCount()    { return rows.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int col) { return COLUMNS[col]; }

    @Override
    public Object getValueAt(int row, int col) {
        Employee e = rows.get(row);
        return switch (col) {
            case 0 -> e.getId();
            case 1 -> e.getLastName();
            case 2 -> e.getFirstName();
            case 3 -> e.getMiddleName();
            case 4 -> e.getPosition();
            case 5 -> e.getPhoneWork();
            case 6 -> e.getPhoneMobile();
            case 7 -> e.getEmail();
            case 8 -> e.getDepartmentName();
            default -> null;
        };
    }
}
