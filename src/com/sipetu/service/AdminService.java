package com.sipetu.service;

import com.sipetu.model.*;
import com.sipetu.repository.DataStore;

import java.util.*;

public class AdminService {
    private final DataStore store;

    public AdminService(DataStore store) {
        this.store = store;
    }

    public synchronized Kelas createKelas(String nama, String deskripsi, Long adminId) {
        String kode = generateKode();
        Kelas k = new Kelas(store.nextKelasId(), nama, deskripsi == null ? "" : deskripsi, kode, adminId);
        store.getClasses().put(k.getId(), k);
        store.persist();
        return k;
    }

    private String generateKode() {
        String kode;
        boolean exists;
        do {
            kode = "SIPT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            String finalKode = kode;
            exists = store.getClasses().values().stream()
                    .anyMatch(c -> c.getKodeEnroll().equalsIgnoreCase(finalKode));
        } while (exists);
        return kode;
    }

    public synchronized List<Kelas> listKelas() {
        return new ArrayList<>(store.getClasses().values());
    }

    public synchronized User createUser(String username, String password, Role role) {
        for (User existing : store.getUsers().values()) {
            if (existing.getUsername().equalsIgnoreCase(username.trim())) {
                return null;
            }
        }
        long id = store.nextUserId();
        String hash = DataStore.hashPassword(password);
        User u = switch (role) {
            case ADMIN -> new Admin(id, username.trim(), hash);
            case SEKRETARIS -> new Sekretaris(id, username.trim(), hash);
            case PELAJAR -> new Pelajar(id, username.trim(), hash);
        };
        store.getUsers().put(u.getId(), u);
        store.persist();
        return u;
    }

    public synchronized User updateUser(Long id, String username, Role role) {
        User old = store.getUsers().get(id);
        if (old == null) return null;
        String newName = (username != null && !username.isBlank()) ? username.trim() : old.getUsername();
        Role newRole = role != null ? role : old.getRole();
        User updated = switch (newRole) {
            case ADMIN -> new Admin(id, newName, old.getPasswordHash());
            case SEKRETARIS -> new Sekretaris(id, newName, old.getPasswordHash());
            case PELAJAR -> new Pelajar(id, newName, old.getPasswordHash());
        };
        store.getUsers().put(id, updated);
        store.persist();
        return updated;
    }

    public synchronized boolean deleteUser(Long id) {
        boolean removed = store.getUsers().remove(id) != null;
        if (removed) {
            store.getEnrollments().values().removeIf(e -> e.getUserId().equals(id));
            store.persist();
        }
        return removed;
    }

    public synchronized List<User> listUsers() {
        return new ArrayList<>(store.getUsers().values());
    }

    public synchronized BidangStudi createBidangStudi(String nama, String kode, Long kelasId) {
        if (!store.getClasses().containsKey(kelasId)) return null;
        BidangStudi bs = new BidangStudi(store.nextBidangId(), nama, kode, kelasId);
        store.getSubjects().put(bs.getId(), bs);
        store.persist();
        return bs;
    }

    public synchronized BidangStudi updateBidangStudi(Long id, String nama, String kode, Long kelasId) {
        BidangStudi bs = store.getSubjects().get(id);
        if (bs == null) return null;
        if (nama != null && !nama.isBlank()) bs.setNama(nama);
        if (kode != null && !kode.isBlank()) bs.setKode(kode);
        if (kelasId != null && store.getClasses().containsKey(kelasId)) bs.setKelasId(kelasId);
        store.persist();
        return bs;
    }

    public synchronized boolean deleteBidangStudi(Long id) {
        boolean removed = store.getSubjects().remove(id) != null;
        if (removed) store.persist();
        return removed;
    }

    public synchronized List<BidangStudi> listBidangStudi() {
        return new ArrayList<>(store.getSubjects().values());
    }
}
