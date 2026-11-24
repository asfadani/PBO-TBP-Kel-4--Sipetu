package com.sipetu.model;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin(Long id, String username, String passwordHash) {
        super(id, username, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    public boolean canManageAdmin() {
        return true;
    }

    @Override
    public boolean canManageSharedTasks() {
        return true;
    }
}
