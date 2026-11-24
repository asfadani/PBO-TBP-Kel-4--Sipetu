package com.sipetu.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class DetailTugas implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long taskId;
    private Long studentId;
    private String status; // "SELESAI" or "BELUM"
    private String keterangan;
    private LocalDateTime updatedAt;

    public DetailTugas(Long id, Long taskId, Long studentId, String status, String keterangan, LocalDateTime updatedAt) {
        this.id = id;
        this.taskId = taskId;
        this.studentId = studentId;
        this.status = status;
        this.keterangan = keterangan;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
