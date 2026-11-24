package com.sipetu.service;

import com.sipetu.model.User;
import com.sipetu.repository.DataStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private final DataStore store;
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    public AuthService(DataStore store) {
        this.store = store;
    }

    public String login(String username, String password) {
        if (username == null || password == null) return null;
        String hash = DataStore.hashPassword(password);
        synchronized (store) {
            for (User u : store.getUsers().values()) {
                if (u.getUsername().equals(username.trim()) && u.getPasswordHash().equals(hash)) {
                    String token = UUID.randomUUID().toString();
                    sessions.put(token, u.getId());
                    return token;
                }
            }
        }
        return null;
    }

    public void logout(String token) {
        if (token != null) sessions.remove(token);
    }

    public User getUserByToken(String token) {
        if (token == null) return null;
        Long userId = sessions.get(token);
        if (userId == null) return null;
        synchronized (store) {
            return store.getUsers().get(userId);
        }
    }
}
