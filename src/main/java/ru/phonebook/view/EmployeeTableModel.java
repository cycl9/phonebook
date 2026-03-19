package ru.phonebook.view;

import ru.phonebook.model.Employee;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель таблицы для JTable — обновляется без прямого обращения к БД.
 *
 * Структура столбцов:
 *   0  «#»             — порядковый номер строки в текущем отображаемом наборе (row + 1)
 *   1  «ID»            — первичный ключ сотрудника из БД
 *   2  «Фамилия»
 *   3  «Имя»
 *   4  «Отчество»
 *   5  «Должность»
 *   6  «Раб.тел.»
 *   7  «Моб.тел.»
 *   8  «Email»
 *   9  «Подразделение»
 *
 * Нумерация строк (#) пересчитывается автоматически при каждом вызове setData(),
 * поэтому поиск/фильтрация всегда показывают актуальные порядковые номера.
 */
public class EmployeeTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
        "#", "ID", "Фамилия", "Имя", "Отчество", "Должность",
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
        if (col == 0) return row + 1;   // порядковый номер строки (1-based)
        Employee e = rows.get(row);
        return switch (col) {
            case 1  -> e.getId();
            case 2  -> e.getLastName();
            case 3  -> e.getFirstName();
            case 4  -> e.getMiddleName();
            case 5  -> e.getPosition();
            case 6  -> e.getPhoneWork();
            case 7  -> e.getPhoneMobile();
            case 8  -> e.getEmail();
            case 9  -> e.getDepartmentName();
            default -> null;
        };
    }
}
