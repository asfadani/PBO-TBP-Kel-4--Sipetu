package com.sipetu.model;

import java.io.Serializable;

/**
 * Abstract base class for all system users (Inheritance & Polymorphism).
 */
public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String passwordHash;

    protected User(Long id, String username, String passwordHash) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username tidak boleh kosong");
        }
        this.id = id;
        this.username = username.trim();
        this.passwordHash = passwordHash;
    }

    public abstract Role getRole();

    public boolean canManageAdmin() { return false; }
    public boolean canManageSharedTasks() { return false; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
