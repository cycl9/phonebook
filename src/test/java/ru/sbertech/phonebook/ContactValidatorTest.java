package ru.sbertech.phonebook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sbertech.phonebook.controller.ContactValidator;
import ru.sbertech.phonebook.model.Employee;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты валидации (9 тестов).
 * Не требуют инициализации Swing или БД.
 */
class ContactValidatorTest {

    private ContactValidator validator;
    private Employee emp;

    @BeforeEach
    void setUp() {
        validator = new ContactValidator();
        emp = new Employee();
        emp.setLastName("Тестов");
        emp.setFirstName("Тест");
        emp.setPhoneMobile("79001234567");
        emp.setDepartmentId(1);
    }

    // ── Тест 1: пустая фамилия ───────────────────────────────────
    @Test
    void validate_emptyLastName_returnsError() {
        emp.setLastName("");
        List<String> errors = validator.validate(emp);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Фамилия")));
    }

    // ── Тест 2: пустое имя ──────────────────────────────────────
    @Test
    void validate_emptyFirstName_returnsError() {
        emp.setFirstName("  ");
        List<String> errors = validator.validate(emp);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Имя")));
    }

    // ── Тест 3: телефон с буквами ────────────────────────────────
    @Test
    void validate_phoneWithLetters_returnsError() {
        emp.setPhoneMobile("7900abc1234");
        List<String> errors = validator.validate(emp);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("телефон")));
    }

    // ── Тест 4: корректный телефон ───────────────────────────────
    @Test
    void validate_correctPhone_noPhoneError() {
        emp.setPhoneMobile("+7 (900) 123-45-67");
        List<String> errors = validator.validate(emp);
        assertTrue(errors.stream().noneMatch(e -> e.contains("телефон")));
    }

    // ── Тест 5: дата рождения в будущем ─────────────────────────
    @Test
    void validate_futureDateOfBirth_returnsError() {
        emp.setDateOfBirth(LocalDate.now().plusYears(1));
        List<String> errors = validator.validate(emp);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Дата рождения")));
    }

    // ── Тест 6: корректная дата рождения ────────────────────────
    @Test
    void validate_pastDateOfBirth_noError() {
        emp.setDateOfBirth(LocalDate.of(1990, 5, 15));
        List<String> errors = validator.validate(emp);
        assertTrue(errors.stream().noneMatch(e -> e.contains("Дата рождения")));
    }

    // ── Тест 7: оба телефона пустые ─────────────────────────────
    @Test
    void validate_bothPhonesEmpty_returnsError() {
        emp.setPhoneMobile(null);
        emp.setPhoneWork(null);
        List<String> errors = validator.validate(emp);
        assertTrue(errors.stream().anyMatch(e -> e.contains("хотя бы один")));
    }

    // ── Тест 8: некорректный email ──────────────────────────────
    @Test
    void validate_invalidEmail_returnsError() {
        emp.setEmail("user_at_corp");   // нет символа @
        List<String> errors = validator.validate(emp);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("почт")));
    }

    // ── Тест 9: полностью корректный сотрудник ───────────────────
    @Test
    void validate_validEmployee_noErrors() {
        emp.setEmail("test@sbertech.ru");
        emp.setDateOfBirth(LocalDate.of(1992, 3, 10));
        assertTrue(validator.isValid(emp));
    }
}
