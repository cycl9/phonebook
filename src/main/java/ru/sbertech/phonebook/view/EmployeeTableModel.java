package ru.sbertech.phonebook.view;

import ru.sbertech.phonebook.model.Employee;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель таблицы для JTable — обновляется без прямого обращения к БД.
 *
 * Столбец 0 («#») — порядковый номер строки в текущем отображаемом наборе (1, 2, 3…).
 * Значение вычисляется как row + 1, всегда соответствует позиции строки в таблице.
 * Если список фильтруется/сбрасывается через setData(), нумерация пересчитывается автоматически.
 * Столбцы 1–8 — поля сотрудника (без ID — ID виден только в модели и используется
 * для delete/edit, но пользователю не показывается).
 */
public class EmployeeTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
        "#", "Фамилия", "Имя", "Отчество", "Должность",
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

    /** Возвращает объект Employee по индексу строки модели (0-based). */
    public Employee getEmployeeAt(int row) { return rows.get(row); }

    @Override public int getRowCount()    { return rows.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int col) { return COLUMNS[col]; }

    @Override
    public Object getValueAt(int row, int col) {
        if (col == 0) return row + 1;          // порядковый номер строки (1-based)
        Employee e = rows.get(row);
        return switch (col) {
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
