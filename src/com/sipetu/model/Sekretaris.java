package com.sipetu.model;

public class Sekretaris extends User {
    private static final long serialVersionUID = 1L;

    public Sekretaris(Long id, String username, String passwordHash) {
        super(id, username, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.SEKRETARIS;
    }

    @Override
    public boolean canManageSharedTasks() {
        return true;
    }
}
