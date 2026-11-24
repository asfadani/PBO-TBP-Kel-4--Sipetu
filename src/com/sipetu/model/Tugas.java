package com.sipetu.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Tugas implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum JenisTugas { BERSAMA, PRIBADI }

    private Long id;
    private String title;
    private String deskripsi;
    private String subject;
    private LocalDate deadline;
    private Priority priority;
    private boolean done;
    private JenisTugas jenis;
    private Long pembuatId;
    private Long pemilikId;
    private Long kelasId;
    private Long bidangStudiId;
    private String tempatPengumpulan;
    private String formatDokumen;

    public Tugas() {
        this.jenis = JenisTugas.BERSAMA;
        this.priority = Priority.MEDIUM;
    }

    public long daysUntilDeadline() {
        return ChronoUnit.DAYS.between(LocalDate.now(), deadline);
    }

    public boolean isOverdue() {
        return !done && deadline.isBefore(LocalDate.now());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public JenisTugas getJenis() { return jenis; }
    public void setJenis(JenisTugas jenis) { this.jenis = jenis; }

    public Long getPembuatId() { return pembuatId; }
    public void setPembuatId(Long pembuatId) { this.pembuatId = pembuatId; }

    public Long getPemilikId() { return pemilikId; }
    public void setPemilikId(Long pemilikId) { this.pemilikId = pemilikId; }

    public Long getKelasId() { return kelasId; }
    public void setKelasId(Long kelasId) { this.kelasId = kelasId; }

    public Long getBidangStudiId() { return bidangStudiId; }
    public void setBidangStudiId(Long bidangStudiId) { this.bidangStudiId = bidangStudiId; }

    public String getTempatPengumpulan() { return tempatPengumpulan; }
    public void setTempatPengumpulan(String tempatPengumpulan) { this.tempatPengumpulan = tempatPengumpulan; }

    public String getFormatDokumen() { return formatDokumen; }
    public void setFormatDokumen(String formatDokumen) { this.formatDokumen = formatDokumen; }
}
