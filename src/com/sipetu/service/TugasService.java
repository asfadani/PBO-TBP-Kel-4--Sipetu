package com.sipetu.service;

import com.sipetu.model.*;
import com.sipetu.repository.DataStore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class TugasService {
    private final DataStore store;

    public TugasService(DataStore store) {
        this.store = store;
    }

    public synchronized Tugas create(String title, String deskripsi, String subject, LocalDate deadline,
                                     Priority priority, Tugas.JenisTugas jenis, Long pembuatId, Long pemilikId,
                                     Long kelasId, Long bidangStudiId, String tempatPengumpulan, String formatDokumen) {
        Tugas t = new Tugas();
        t.setId(store.nextTugasId());
        t.setTitle(title);
        t.setDeskripsi(deskripsi == null ? "" : deskripsi);
        t.setSubject(subject);
        t.setDeadline(deadline);
        t.setPriority(priority != null ? priority : Priority.MEDIUM);
        t.setDone(false);
        t.setJenis(jenis);
        t.setPembuatId(pembuatId);
        t.setPemilikId(pemilikId);
        t.setKelasId(kelasId);
        t.setBidangStudiId(bidangStudiId);
        t.setTempatPengumpulan(tempatPengumpulan == null ? "" : tempatPengumpulan);
        t.setFormatDokumen(formatDokumen == null ? "" : formatDokumen);
        store.getTasks().put(t.getId(), t);
        store.persist();
        return t;
    }

    public synchronized Tugas update(Long id, String title, String deskripsi, String subject, LocalDate deadline,
                                     Priority priority, String tempatPengumpulan, String formatDokumen) {
        Tugas t = store.getTasks().get(id);
        if (t == null) return null;
        if (title != null && !title.isBlank()) t.setTitle(title);
        if (deskripsi != null) t.setDeskripsi(deskripsi);
        if (subject != null && !subject.isBlank()) t.setSubject(subject);
        if (deadline != null) t.setDeadline(deadline);
        if (priority != null) t.setPriority(priority);
        if (tempatPengumpulan != null) t.setTempatPengumpulan(tempatPengumpulan);
        if (formatDokumen != null) t.setFormatDokumen(formatDokumen);
        store.persist();
        return t;
    }

    public synchronized boolean delete(Long id) {
        boolean removed = store.getTasks().remove(id) != null;
        if (removed) {
            store.getDetails().values().removeIf(d -> d.getTaskId().equals(id));
            store.persist();
        }
        return removed;
    }

    public synchronized Tugas findById(Long id) {
        return store.getTasks().get(id);
    }

    public synchronized List<Tugas> findSharedByKelasIds(List<Long> kelasIds) {
        if (kelasIds == null || kelasIds.isEmpty()) return List.of();
        List<Tugas> result = new ArrayList<>();
        for (Tugas t : store.getTasks().values()) {
            if (t.getJenis() == Tugas.JenisTugas.BERSAMA && t.getKelasId() != null && kelasIds.contains(t.getKelasId())) {
                result.add(t);
            }
        }
        return result;
    }

    public synchronized List<Tugas> findPersonalByUser(Long userId) {
        List<Tugas> result = new ArrayList<>();
        for (Tugas t : store.getTasks().values()) {
            if (t.getJenis() == Tugas.JenisTugas.PRIBADI && userId.equals(t.getPemilikId())) {
                result.add(t);
            }
        }
        return result;
    }

    public synchronized boolean setStatus(Long taskId, Long userId, String status, String keterangan) {
        Tugas t = store.getTasks().get(taskId);
        if (t == null) return false;
        String normStatus = "SELESAI".equalsIgnoreCase(status) ? "SELESAI" : "BELUM";
        if (t.getJenis() == Tugas.JenisTugas.PRIBADI) {
            t.setDone("SELESAI".equals(normStatus));
        }
        DetailTugas existing = findDetail(taskId, userId);
        if (existing == null) {
            DetailTugas d = new DetailTugas(store.nextDetailId(), taskId, userId, normStatus,
                    keterangan == null ? "" : keterangan, LocalDateTime.now());
            store.getDetails().put(d.getId(), d);
        } else {
            existing.setStatus(normStatus);
            if (keterangan != null) existing.setKeterangan(keterangan);
            existing.setUpdatedAt(LocalDateTime.now());
        }
        store.persist();
        return true;
    }

    private DetailTugas findDetail(Long taskId, Long studentId) {
        for (DetailTugas d : store.getDetails().values()) {
            if (d.getTaskId().equals(taskId) && d.getStudentId().equals(studentId)) {
                return d;
            }
        }
        return null;
    }

    public synchronized String getStudentStatus(Long taskId, Long studentId) {
        DetailTugas d = findDetail(taskId, studentId);
        if (d != null) return d.getStatus();
        Tugas t = store.getTasks().get(taskId);
        if (t != null && t.getJenis() == Tugas.JenisTugas.PRIBADI) {
            return t.isDone() ? "SELESAI" : "BELUM";
        }
        return "BELUM";
    }

    public synchronized long countStatus(Long taskId, String status) {
        long count = 0;
        for (DetailTugas d : store.getDetails().values()) {
            if (d.getTaskId().equals(taskId) && status.equalsIgnoreCase(d.getStatus())) {
                User u = store.getUsers().get(d.getStudentId());
                if (u != null && (u.getRole() == Role.PELAJAR || u.getRole() == Role.SEKRETARIS)) {
                    count++;
                }
            }
        }
        return count;
    }
}
