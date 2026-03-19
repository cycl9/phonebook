package ru.sbertech.phonebook.dao;

import ru.sbertech.phonebook.model.AppUser;
import java.util.Optional;

public interface AppUserDao {
    Optional<AppUser> findByUsername(String username);
}
