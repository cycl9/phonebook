package ru.sbertech.phonebook.view;

import ru.sbertech.phonebook.controller.AppController;
import ru.sbertech.phonebook.model.Employee;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class MainFrame extends JFrame {

    private final AppController      controller;
    private final EmployeeTableModel tableModel = new EmployeeTableModel();
    private final JTable             table      = new JTable(tableModel);

    // Поля поиска
    private final JTextField tfSearch = new JTextField(18); // имя/фамилия/телефон
    private final JTextField tfDept   = new JTextField(14); // подразделение
    private final JLabel     lblCount = new JLabel("Записей: 0");

    // Кнопки admin
    private final JButton btnLogin  = new JButton("Войти как администратор");
    private final JButton btnLogout = new JButton("Выйти");
    private final JButton btnAdd    = new JButton("Добавить");
    private final JButton btnEdit   = new JButton("Редактировать");
    private final JButton btnDelete = new JButton("Удалить");
    private final JLabel  lblRole   = new JLabel("Роль: Пользователь");

    public MainFrame(AppController controller) {
        super("Телефонная адресная книга — АО \"СБЕРТЕХ\"");
        this.controller = controller;
        buildUI();
        refreshTable(controller.findAll());
        updateAdminUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 650);
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        add(buildTopPanel(), BorderLayout.NORTH);
        add(new JScrollPane(buildTable()), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && controller.isAdminSession()) onEdit();
            }
        });

        btnLogin.addActionListener(e  -> onLogin());
        btnLogout.addActionListener(e -> onLogout());
        btnAdd.addActionListener(e    -> onAdd());
        btnEdit.addActionListener(e   -> onEdit());
        btnDelete.addActionListener(e -> onDelete());
    }

    private JPanel buildTopPanel() {
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));

        // Главный поиск — имя, фамилия, телефон (всё в одном)
        JLabel lSearch = bold("Поиск (имя / фамилия / телефон):");
        searchRow.add(lSearch);
        searchRow.add(tfSearch);

        searchRow.add(Box.createHorizontalStrut(10));
        searchRow.add(bold("Подразделение:"));
        searchRow.add(tfDept);

        JButton btnSearch = new JButton("Найти");
        JButton btnReset  = new JButton("Сбросить");
        btnSearch.setPreferredSize(new Dimension(90, 26));
        btnReset.setPreferredSize(new Dimension(90, 26));
        searchRow.add(btnSearch);
        searchRow.add(btnReset);

        btnSearch.addActionListener(e -> onSearch());
        btnReset.addActionListener(e  -> onReset());

        // Enter в любом поле
        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) onSearch();
            }
        };
        tfSearch.addKeyListener(enter);
        tfDept.addKeyListener(enter);
        getRootPane().setDefaultButton(btnSearch);

        // Подсказка под полем
        JLabel hint = new JLabel(
            "<html><font color='gray' size='2'>" +
            "Примеры: «Иван», «Сидоров», «Сидоров Алексей», «79161112233»" +
            "</font></html>");

        JPanel searchCol = new JPanel(new BorderLayout(0, 0));
        searchCol.add(searchRow, BorderLayout.NORTH);
        searchCol.add(hint,      BorderLayout.SOUTH);

        // Admin row
        JPanel adminRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        lblRole.setFont(lblRole.getFont().deriveFont(Font.BOLD, 12f));
        adminRow.add(lblRole);
        adminRow.add(btnLogin); adminRow.add(btnLogout);
        adminRow.add(new JSeparator(SwingConstants.VERTICAL));
        adminRow.add(btnAdd); adminRow.add(btnEdit); adminRow.add(btnDelete);

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Поиск"),
            new EmptyBorder(0, 4, 2, 4)));
        top.add(searchCol, BorderLayout.CENTER);
        top.add(adminRow,  BorderLayout.EAST);
        return top;
    }

    private JTable buildTable() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        // Столбец «#» — порядковый номер, узкий
        table.getColumnModel().getColumn(0).setMinWidth(30);
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(0).setPreferredWidth(35);
        return table;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(new EmptyBorder(2, 8, 2, 8));
        JLabel hint = new JLabel(
            "Двойной клик — редактировать (только администратор)");
        hint.setForeground(Color.GRAY);
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
        lblCount.setForeground(new Color(0, 110, 0));
        bar.add(hint, BorderLayout.CENTER);
        bar.add(lblCount, BorderLayout.EAST);
        return bar;
    }

    private void onSearch() {
        String query = tfSearch.getText().trim();
        String dept  = tfDept.getText().trim();

        List<Employee> results;
        if (!dept.isEmpty()) {
            // Подразделение имеет приоритет при заполнении
            results = !query.isEmpty()
                ? filterByDeptAndQuery(dept, query)
                : controller.findByDepartment(dept);
        } else if (!query.isEmpty()) {
            // Универсальный поиск: одно слово или "Фамилия Имя"
            results = controller.searchEmployees(query);
        } else {
            results = controller.findAll();
        }

        refreshTable(results);

        if (results.isEmpty() && (!query.isEmpty() || !dept.isEmpty())) {
            String what = !query.isEmpty() ? "«" + query + "»" : "«" + dept + "»";
            JOptionPane.showMessageDialog(this, "По запросу " + what + " ничего не найдено.",
                "Поиск", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Фильтрация делегируется контроллеру — вся логика поиска сосредоточена там */
    private List<Employee> filterByDeptAndQuery(String dept, String query) {
        return controller.searchEmployees(query, dept);
    }

    private void onReset() {
        tfSearch.setText(""); tfDept.setText("");
        refreshTable(controller.findAll());
        tfSearch.requestFocus();
    }

    private void onLogin() {
        LoginDialog dlg = new LoginDialog(this, controller);
        dlg.setVisible(true);
        if (dlg.isSuccess()) updateAdminUI();
    }

    private void onLogout() { controller.logout(); updateAdminUI(); }

    private void onAdd() {
        EmployeeDialog dlg = new EmployeeDialog(this, controller, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) refreshTable(controller.findAll());
    }

    private void onEdit() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Выберите сотрудника.", "Редактирование", JOptionPane.WARNING_MESSAGE);
            return;
        }
        EmployeeDialog dlg = new EmployeeDialog(this, controller, copyOf(tableModel.getEmployeeAt(row)));
        dlg.setVisible(true);
        if (dlg.isSaved()) refreshTable(controller.findAll());
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Выберите сотрудника.", "Удаление", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Employee emp = tableModel.getEmployeeAt(row);
        int ok = JOptionPane.showConfirmDialog(this,
            "Удалить «" + emp.getFullName() + "»?",
            "Подтверждение", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            controller.deleteEmployee(emp.getId());
            refreshTable(controller.findAll());
        }
    }

    private void refreshTable(List<Employee> data) {
        tableModel.setData(data);
        lblCount.setText("Записей: " + data.size());
    }

    private void updateAdminUI() {
        boolean admin = controller.isAdminSession();
        btnLogin.setVisible(!admin); btnLogout.setVisible(admin);
        btnAdd.setVisible(admin); btnEdit.setVisible(admin); btnDelete.setVisible(admin);
        lblRole.setText(admin ? "Роль: Администратор" : "Роль: Пользователь");
        lblRole.setForeground(admin ? new Color(0, 128, 0) : Color.DARK_GRAY);
        revalidate(); repaint();
    }

    private JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        return l;
    }

    private Employee copyOf(Employee src) {
        Employee e = new Employee();
        e.setId(src.getId()); e.setLastName(src.getLastName()); e.setFirstName(src.getFirstName());
        e.setMiddleName(src.getMiddleName()); e.setPosition(src.getPosition());
        e.setPhoneWork(src.getPhoneWork()); e.setPhoneMobile(src.getPhoneMobile());
        e.setEmail(src.getEmail()); e.setDateOfBirth(src.getDateOfBirth());
        e.setDepartmentId(src.getDepartmentId()); e.setDepartmentName(src.getDepartmentName());
        return e;
    }
}
