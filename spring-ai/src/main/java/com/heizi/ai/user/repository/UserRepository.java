package com.heizi.ai.user.repository;

import java.util.Optional;

import com.heizi.ai.user.model.User;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

}
