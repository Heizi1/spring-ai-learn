package com.heizi.ai.user.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.heizi.ai.user.model.User;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final AtomicLong idGenerator = new AtomicLong(1000);
    private final ConcurrentMap<Long, User> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> usernameIndex = new ConcurrentHashMap<>();

    @Override
    public synchronized User save(User user) {
        Instant now = Instant.now();
        if (user.getId() == null) {
            user.setId(idGenerator.incrementAndGet());
            user.setCreatedAt(now);
        }
        user.setUpdatedAt(now);
        users.put(user.getId(), user);
        usernameIndex.put(normalizeUsername(user.getUsername()), user.getId());
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        Long userId = usernameIndex.get(normalizeUsername(username));
        if (userId == null) {
            return Optional.empty();
        }
        return findById(userId);
    }

    @Override
    public boolean existsByUsername(String username) {
        return usernameIndex.containsKey(normalizeUsername(username));
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

}
