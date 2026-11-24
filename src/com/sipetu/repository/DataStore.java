package com.sipetu.repository;

import com.sipetu.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe persistent data store backed by a local file (data/sipetu.dat).
 */
public class DataStore implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DATA_FILE = "data/sipetu.dat";

    private final AtomicLong userSeq = new AtomicLong(1);
    private final AtomicLong kelasSeq = new AtomicLong(1);
    private final AtomicLong bidangSeq = new AtomicLong(1);
    private final AtomicLong enrollSeq = new AtomicLong(1);
    private final AtomicLong tugasSeq = new AtomicLong(1);
    private final AtomicLong detailSeq = new AtomicLong(1);

    private final Map<Long, User> users = new LinkedHashMap<>();
    private final Map<Long, Kelas> classes = new LinkedHashMap<>();
    private final Map<Long, BidangStudi> subjects = new LinkedHashMap<>();
    private final Map<Long, EnrollmentKelas> enrollments = new LinkedHashMap<>();
    private final Map<Long, Tugas> tasks = new LinkedHashMap<>();
    private final Map<Long, DetailTugas> details = new LinkedHashMap<>();

    public static synchronized DataStore loadOrCreate() {
        File f = new File(DATA_FILE);
        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                DataStore ds = (DataStore) ois.readObject();
                System.out.println("[DataStore] Loaded existing data from " + DATA_FILE);
                return ds;
            } catch (Exception e) {
                System.err.println("[DataStore] Failed to load data, creating fresh store: " + e.getMessage());
            }
        }
        DataStore ds = new DataStore();
        ds.seedInitialData();
        ds.persist();
        return ds;
    }

    public synchronized void persist() {
        File f = new File(DATA_FILE);
        if (f.getParentFile() != null) {
            f.getParentFile().mkdirs();
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
            oos.writeObject(this);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save DataStore", e);
        }
    }

    public static String hashPassword(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void seedInitialData() {
        Admin admin = new Admin(userSeq.getAndIncrement(), "admin", hashPassword("admin123"));
        Sekretaris sekr = new Sekretaris(userSeq.getAndIncrement(), "sekretaris", hashPassword("sekretaris123"));
        Pelajar pelajar = new Pelajar(userSeq.getAndIncrement(), "pelajar", hashPassword("pelajar123"));
        users.put(admin.getId(), admin);
        users.put(sekr.getId(), sekr);
        users.put(pelajar.getId(), pelajar);

        Kelas kelas = new Kelas(kelasSeq.getAndIncrement(), "TI-2025-A", "Kelas Reguler TI", "SIPT-ABC123", admin.getId());
        classes.put(kelas.getId(), kelas);

        BidangStudi mat = new BidangStudi(bidangSeq.getAndIncrement(), "Matematika", "MAT", kelas.getId());
        subjects.put(mat.getId(), mat);

        System.out.println("[Seed] Akun awal (admin/sekretaris/pelajar), kelas TI-2025-A (SIPT-ABC123), dan bidang studi Matematika dibuat. Tidak ada tugas awal.");
    }

    public synchronized long nextUserId() { return userSeq.getAndIncrement(); }
    public synchronized long nextKelasId() { return kelasSeq.getAndIncrement(); }
    public synchronized long nextBidangId() { return bidangSeq.getAndIncrement(); }
    public synchronized long nextEnrollId() { return enrollSeq.getAndIncrement(); }
    public synchronized long nextTugasId() { return tugasSeq.getAndIncrement(); }
    public synchronized long nextDetailId() { return detailSeq.getAndIncrement(); }

    public synchronized Map<Long, User> getUsers() { return users; }
    public synchronized Map<Long, Kelas> getClasses() { return classes; }
    public synchronized Map<Long, BidangStudi> getSubjects() { return subjects; }
    public synchronized Map<Long, EnrollmentKelas> getEnrollments() { return enrollments; }
    public synchronized Map<Long, Tugas> getTasks() { return tasks; }
    public synchronized Map<Long, DetailTugas> getDetails() { return details; }
}
