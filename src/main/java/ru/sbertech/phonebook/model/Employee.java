package ru.sbertech.phonebook.model;

import java.time.LocalDate;

public class Employee {
    private int id;
    private String lastName;
    private String firstName;
    private String middleName;
    private String position;
    private String phoneWork;
    private String phoneMobile;
    private String email;
    private LocalDate dateOfBirth;
    private int departmentId;
    private String departmentName; // денормализованное поле для отображения

    public Employee() {}

    // геттеры и сеттеры
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }
    public String getLastName()                 { return lastName; }
    public void setLastName(String v)           { this.lastName = v; }
    public String getFirstName()                { return firstName; }
    public void setFirstName(String v)          { this.firstName = v; }
    public String getMiddleName()               { return middleName; }
    public void setMiddleName(String v)         { this.middleName = v; }
    public String getPosition()                 { return position; }
    public void setPosition(String v)           { this.position = v; }
    public String getPhoneWork()                { return phoneWork; }
    public void setPhoneWork(String v)          { this.phoneWork = v; }
    public String getPhoneMobile()              { return phoneMobile; }
    public void setPhoneMobile(String v)        { this.phoneMobile = v; }
    public String getEmail()                    { return email; }
    public void setEmail(String v)              { this.email = v; }
    public LocalDate getDateOfBirth()           { return dateOfBirth; }
    public void setDateOfBirth(LocalDate v)     { this.dateOfBirth = v; }
    public int getDepartmentId()                { return departmentId; }
    public void setDepartmentId(int v)          { this.departmentId = v; }
    public String getDepartmentName()           { return departmentName; }
    public void setDepartmentName(String v)     { this.departmentName = v; }

    public String getFullName() {
        String ln = lastName  != null ? lastName  : "";
        String fn = firstName != null ? firstName : "";
        String mn = middleName != null && !middleName.isBlank() ? " " + middleName : "";
        return (ln + " " + fn + mn).trim();
    }
}
