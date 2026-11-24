package com.sipetu.service;

import com.sipetu.model.*;
import com.sipetu.repository.DataStore;

import java.util.*;

public class KelasService {
    public static final int ENROLL_OK = 0;
    public static final int ENROLL_NOT_FOUND = 1;
    public static final int ENROLL_ALREADY = 2;

    private final DataStore store;

    public KelasService(DataStore store) {
        this.store = store;
    }

    public synchronized int enroll(Long userId, String kodeEnroll) {
        if (kodeEnroll == null || kodeEnroll.isBlank()) return ENROLL_NOT_FOUND;
        String cleanKode = kodeEnroll.trim();
        Kelas target = null;
        for (Kelas k : store.getClasses().values()) {
            if (k.getKodeEnroll().equalsIgnoreCase(cleanKode)) {
                target = k;
                break;
            }
        }
        if (target == null) return ENROLL_NOT_FOUND;

        for (EnrollmentKelas e : store.getEnrollments().values()) {
            if (e.getClassId().equals(target.getId()) && e.getUserId().equals(userId)) {
                return ENROLL_ALREADY;
            }
        }

        // Single-class enrollment per PRD: replace any previous enrollment if switching class, or keep simple
        EnrollmentKelas enrollment = new EnrollmentKelas(store.nextEnrollId(), target.getId(), userId);
        store.getEnrollments().put(enrollment.getId(), enrollment);
        store.persist();
        return ENROLL_OK;
    }

    public synchronized List<Kelas> findByUserId(Long userId) {
        List<Kelas> result = new ArrayList<>();
        for (EnrollmentKelas e : store.getEnrollments().values()) {
            if (e.getUserId().equals(userId)) {
                Kelas k = store.getClasses().get(e.getClassId());
                if (k != null) result.add(k);
            }
        }
        return result;
    }

    public synchronized List<Long> findKelasIdsByUserId(Long userId) {
        List<Long> ids = new ArrayList<>();
        for (EnrollmentKelas e : store.getEnrollments().values()) {
            if (e.getUserId().equals(userId)) {
                ids.add(e.getClassId());
            }
        }
        return ids;
    }

    public synchronized long countPelajarByClassId(Long classId) {
        if (classId == null) return 0;
        long count = 0;
        for (EnrollmentKelas e : store.getEnrollments().values()) {
            if (e.getClassId().equals(classId)) {
                User u = store.getUsers().get(e.getUserId());
                if (u != null && (u.getRole() == Role.PELAJAR || u.getRole() == Role.SEKRETARIS)) {
                    count++;
                }
            }
        }
        return count;
    }
}
