package ru.sbertech.phonebook.model;

public class AppUser {
    private int id;
    private String username;
    private String passwordHash;

    public AppUser() {}
    public AppUser(int id, String username, String passwordHash) {
        this.id = id; this.username = username; this.passwordHash = passwordHash;
    }

    public int getId()                      { return id; }
    public void setId(int id)              { this.id = id; }
    public String getUsername()             { return username; }
    public void setUsername(String v)       { this.username = v; }
    public String getPasswordHash()         { return passwordHash; }
    public void setPasswordHash(String v)   { this.passwordHash = v; }
}
