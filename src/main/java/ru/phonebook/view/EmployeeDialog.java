package ru.phonebook.view;

import ru.phonebook.controller.AppController;
import ru.phonebook.model.Department;
import ru.phonebook.model.Employee;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Модальный диалог добавления / редактирования сотрудника (FR-5, FR-6).
 */
public class EmployeeDialog extends JDialog {

    private final AppController controller;
    private Employee employee;
    private boolean saved = false;

    private final JTextField fLastName    = new JTextField(20);
    private final JTextField fFirstName   = new JTextField(20);
    private final JTextField fMiddleName  = new JTextField(20);
    private final JTextField fPosition    = new JTextField(20);
    private final JTextField fPhoneWork   = new JTextField(20);
    private final JTextField fPhoneMobile = new JTextField(20);
    private final JTextField fEmail       = new JTextField(20);
    private final JTextField fDateOfBirth = new JTextField(20);
    private final JComboBox<Department> cbDept = new JComboBox<>();
    private final JLabel errorLabel = new JLabel(" ");

    public EmployeeDialog(Frame parent, AppController controller, Employee employee) {
        super(parent, employee == null ? "Добавить сотрудника" : "Редактировать сотрудника", true);
        this.controller = controller;
        this.employee = employee == null ? new Employee() : employee;
        buildUI();
        populate();
        pack();
        setMinimumSize(new Dimension(420, 460));
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8);
        c.anchor = GridBagConstraints.WEST;

        String[] labels = {
            "<html>Фамилия <font color='red'>*</font></html>",
            "<html>Имя <font color='red'>*</font></html>",
            "Отчество",
            "Должность",
            "Раб. телефон",
            "Моб. телефон",
            "Email",
            "Дата рождения (ГГГГ-ММ-ДД)",
            "<html>Подразделение <font color='red'>*</font></html>"
        };
        JComponent[] fields = {fLastName, fFirstName, fMiddleName, fPosition,
                                fPhoneWork, fPhoneMobile, fEmail, fDateOfBirth, cbDept};

        for (int i = 0; i < labels.length; i++) {
            c.gridx = 0; c.gridy = i; c.weightx = 0;
            form.add(new JLabel(labels[i]), c);
            c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
            form.add(fields[i], c);
        }

        // Подразделения
        for (Department d : controller.getDepartments()) cbDept.addItem(d);

        // Ошибки — выделенная панель
        errorLabel.setForeground(new Color(180, 0, 0));
        errorLabel.setFont(errorLabel.getFont().deriveFont(Font.BOLD, 11f));
        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.setBorder(new EmptyBorder(2, 8, 2, 8));
        errorPanel.add(errorLabel, BorderLayout.CENTER);

        c.gridx = 0; c.gridy = labels.length; c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(errorPanel, c);

        // Подсказка под формой
        JLabel hint = new JLabel(
            "<html><font color='gray'><i>" +
            "<font color='red'>*</font> — обязательные поля.&nbsp;&nbsp;" +
            "Необходимо указать хотя бы один телефон." +
            "</i></font></html>");
        hint.setBorder(new EmptyBorder(6, 8, 4, 8));

        // Кнопки
        JButton saveBtn   = new JButton("Сохранить");
        JButton cancelBtn = new JButton("Отмена");
        saveBtn.setPreferredSize(new Dimension(110, 28));
        cancelBtn.setPreferredSize(new Dimension(90, 28));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        buttons.add(cancelBtn);
        buttons.add(saveBtn);

        saveBtn.addActionListener(e -> onSave());
        cancelBtn.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(saveBtn);

        JPanel south = new JPanel(new BorderLayout());
        south.add(hint,    BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(form,  BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private void populate() {
        fLastName.setText(orEmpty(employee.getLastName()));
        fFirstName.setText(orEmpty(employee.getFirstName()));
        fMiddleName.setText(orEmpty(employee.getMiddleName()));
        fPosition.setText(orEmpty(employee.getPosition()));
        fPhoneWork.setText(orEmpty(employee.getPhoneWork()));
        fPhoneMobile.setText(orEmpty(employee.getPhoneMobile()));
        fEmail.setText(orEmpty(employee.getEmail()));
        fDateOfBirth.setText(employee.getDateOfBirth() != null ? employee.getDateOfBirth().toString() : "");

        // Выбрать нужное подразделение в ComboBox
        for (int i = 0; i < cbDept.getItemCount(); i++) {
            if (cbDept.getItemAt(i).getId() == employee.getDepartmentId()) {
                cbDept.setSelectedIndex(i);
                break;
            }
        }
    }

    private void onSave() {
        employee.setLastName(fLastName.getText().trim());
        employee.setFirstName(fFirstName.getText().trim());
        employee.setMiddleName(fMiddleName.getText().trim());
        employee.setPosition(fPosition.getText().trim());
        employee.setPhoneWork(fPhoneWork.getText().trim());
        employee.setPhoneMobile(fPhoneMobile.getText().trim());
        employee.setEmail(fEmail.getText().trim());

        String dob = fDateOfBirth.getText().trim();
        if (!dob.isEmpty()) {
            try { employee.setDateOfBirth(LocalDate.parse(dob)); }
            catch (DateTimeParseException ex) {
                errorLabel.setText("Формат даты рождения: ГГГГ-ММ-ДД");
                return;
            }
        } else {
            employee.setDateOfBirth(null);
        }

        Department dept = (Department) cbDept.getSelectedItem();
        if (dept == null) {
            errorLabel.setText("Необходимо выбрать подразделение.");
            return;
        }
        employee.setDepartmentId(dept.getId());

        List<String> errors = employee.getId() == 0
                ? controller.addEmployee(employee)
                : controller.updateEmployee(employee);

        if (errors.isEmpty()) {
            saved = true;
            dispose();
        } else {
            errorLabel.setText("<html>" + String.join("<br>", errors) + "</html>");
        }
    }

    public boolean isSaved() { return saved; }
    public Employee getEmployee() { return employee; }

    private static String orEmpty(String s) { return s == null ? "" : s; }
}
