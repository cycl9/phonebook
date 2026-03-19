package ru.phonebook.dao;

import ru.phonebook.model.AppUser;
import java.util.Optional;

public interface AppUserDao {
    Optional<AppUser> findByUsername(String username);
}
