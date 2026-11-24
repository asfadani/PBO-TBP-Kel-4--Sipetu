package com.sipetu.model;

public class Pelajar extends User {
    private static final long serialVersionUID = 1L;

    public Pelajar(Long id, String username, String passwordHash) {
        super(id, username, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.PELAJAR;
    }
}
