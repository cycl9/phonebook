package ru.sbertech.phonebook.view;

import ru.sbertech.phonebook.controller.AppController;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Диалог аутентификации администратора (FR-8).
 */
public class LoginDialog extends JDialog {

    private final AppController controller;
    private boolean success = false;

    public LoginDialog(Frame parent, AppController controller) {
        super(parent, "Вход для администратора", true);
        this.controller = controller;
        buildUI();
        pack();
        setMinimumSize(new Dimension(360, 260));
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBorder(new EmptyBorder(20, 28, 16, 28));

        // ── Форма ────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets  = new Insets(6, 6, 6, 6);
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;

        JTextField     userField = new JTextField(18);
        JPasswordField passField = new JPasswordField(18);

        // Логин
        c.gridx = 0; c.gridy = 0; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        form.add(new JLabel("Логин:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        form.add(userField, c);

        // Пароль
        c.gridx = 0; c.gridy = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        form.add(new JLabel("Пароль:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        form.add(passField, c);

        // Ошибка
        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(errorLabel.getFont().deriveFont(Font.PLAIN, 12f));
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        form.add(errorLabel, c);

        // ── Кнопка ───────────────────────────────────────────────
        JButton loginBtn = new JButton("Войти");
        loginBtn.setPreferredSize(new Dimension(120, 32));
        JPanel btnPanel = new JPanel();
        btnPanel.add(loginBtn);

        // ── Сборка ───────────────────────────────────────────────
        root.add(form,     BorderLayout.CENTER);
        root.add(btnPanel, BorderLayout.SOUTH);

        loginBtn.addActionListener(e -> {
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword());
            if (user.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("Введите логин и пароль.");
                return;
            }
            if (controller.login(user, pass)) {
                success = true;
                dispose();
            } else {
                errorLabel.setText("Неверный логин или пароль.");
                passField.setText("");
                userField.requestFocus();
            }
        });

        // Enter → войти
        getRootPane().setDefaultButton(loginBtn);

        // Автофокус на поле логина
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowOpened(java.awt.event.WindowEvent e) {
                userField.requestFocusInWindow();
            }
        });

        setContentPane(root);
    }

    public boolean isSuccess() { return success; }
}
