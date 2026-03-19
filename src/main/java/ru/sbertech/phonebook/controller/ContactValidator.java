package ru.sbertech.phonebook.controller;

import ru.sbertech.phonebook.model.Employee;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Валидация полей формы сотрудника (реализует требование NFR-3).
 */
public class ContactValidator {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+\\d()\\-\\s]{7,20}$");
    private static final Pattern EMAIL_PATTERN  = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /** Возвращает список ошибок; пустой список — валидация прошла. */
    public List<String> validate(Employee emp) {
        List<String> errors = new ArrayList<>();

        if (isBlank(emp.getLastName()))  errors.add("Фамилия обязательна для заполнения.");
        if (isBlank(emp.getFirstName())) errors.add("Имя обязательно для заполнения.");
        if (emp.getDepartmentId() <= 0)  errors.add("Необходимо выбрать подразделение.");

        if (!isBlank(emp.getPhoneWork()) && !PHONE_PATTERN.matcher(emp.getPhoneWork()).matches())
            errors.add("Рабочий телефон содержит недопустимые символы.");
        if (!isBlank(emp.getPhoneMobile()) && !PHONE_PATTERN.matcher(emp.getPhoneMobile()).matches())
            errors.add("Мобильный телефон содержит недопустимые символы.");

        if (isBlank(emp.getPhoneWork()) && isBlank(emp.getPhoneMobile()))
            errors.add("Необходимо указать хотя бы один номер телефона.");

        if (!isBlank(emp.getEmail()) && !EMAIL_PATTERN.matcher(emp.getEmail()).matches())
            errors.add("Некорректный формат адреса электронной почты.");

        if (emp.getDateOfBirth() != null && emp.getDateOfBirth().isAfter(LocalDate.now()))
            errors.add("Дата рождения не может быть в будущем.");

        return errors;
    }

    public boolean isValid(Employee emp) { return validate(emp).isEmpty(); }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
