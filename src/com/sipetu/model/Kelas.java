package com.sipetu.model;

import java.io.Serializable;

public class Kelas implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String nama;
    private String deskripsi;
    private String kodeEnroll;
    private Long adminId;

    public Kelas(Long id, String nama, String deskripsi, String kodeEnroll, Long adminId) {
        this.id = id;
        this.nama = nama;
        this.deskripsi = deskripsi;
        this.kodeEnroll = kodeEnroll;
        this.adminId = adminId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }

    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    public String getKodeEnroll() { return kodeEnroll; }
    public void setKodeEnroll(String kodeEnroll) { this.kodeEnroll = kodeEnroll; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
}
