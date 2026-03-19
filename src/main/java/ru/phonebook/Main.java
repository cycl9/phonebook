package ru.phonebook;

import ru.phonebook.controller.AppController;
import ru.phonebook.dao.*;
import ru.phonebook.util.DbInitializer;
import ru.phonebook.view.MainFrame;
import javax.swing.*;

/**
 * Точка входа. Инициализирует БД, создаёт DAOs, контроллер и запускает GUI.
 */
public class Main {
    private static final String DB_URL = "jdbc:sqlite:phonebook.db";

    public static void main(String[] args) {
        // Инициализация БД (таблицы + тестовые данные)
        DbInitializer.initialize(DB_URL);

        // Создание DAO
        ContactDao    contactDao    = new SQLiteContactDao(DB_URL);
        DepartmentDao departmentDao = new SQLiteDepartmentDao(DB_URL);
        AppUserDao    userDao       = new SQLiteAppUserDao(DB_URL);

        // Контроллер
        AppController controller = new AppController(contactDao, departmentDao, userDao);

        // Запуск GUI в потоке Swing EDT
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new MainFrame(controller).setVisible(true);
        });
    }
}
