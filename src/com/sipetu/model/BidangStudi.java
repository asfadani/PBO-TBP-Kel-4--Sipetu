package com.sipetu.model;

import java.io.Serializable;

public class BidangStudi implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String nama;
    private String kode;
    private Long kelasId;

    public BidangStudi(Long id, String nama, String kode, Long kelasId) {
        this.id = id;
        this.nama = nama;
        this.kode = kode;
        this.kelasId = kelasId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }

    public String getKode() { return kode; }
    public void setKode(String kode) { this.kode = kode; }

    public Long getKelasId() { return kelasId; }
    public void setKelasId(Long kelasId) { this.kelasId = kelasId; }
}
