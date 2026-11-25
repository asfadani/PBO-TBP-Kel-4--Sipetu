package com.sipetu.test;

import com.sipetu.model.*;
import com.sipetu.repository.DataStore;
import com.sipetu.service.*;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Pengujian fitur SIPETU V3 (skenario berhasil dan gagal per fitur).
 */
public class SipetuTest {

    static void cek(boolean kondisi, String pesan) {
        if (!kondisi) throw new AssertionError("GAGAL: " + pesan);
    }

    public static void main(String[] args) {
        File f = new File("data/sipetu.dat");
        if (f.exists()) f.delete();

        DataStore store = DataStore.loadOrCreate();
        AuthService auth = new AuthService(store);
        AdminService admin = new AdminService(store);
        KelasService kelasSvc = new KelasService(store);
        TugasService tugasSvc = new TugasService(store);
        NotifikasiPengingat pengingat = new PengingatService(tugasSvc, kelasSvc);

        // 1. Uji Validasi Encapsulation (Gagal jika kosong)
        boolean errJudul = false;
        try {
            Tugas tErr = new Tugas();
            tErr.setTitle("   ");
        } catch (IllegalArgumentException e) { errJudul = true; }
        cek(errJudul, "Judul tugas kosong harus ditolak");

        boolean errUser = false;
        try { new Pelajar(99L, "", "hash"); } catch (IllegalArgumentException e) { errUser = true; }
        cek(errUser, "Username kosong harus ditolak");

        // 2. Uji Autentikasi (Berhasil & Gagal)
        String tokenSekr = auth.login("sekretaris", "sekretaris123");
        cek(tokenSekr != null, "Login sekretaris valid harus berhasil");
        cek(auth.login("sekretaris", "salah") == null, "Login password salah harus gagal");

        // 3. Uji Enroll Kelas (Berhasil & Gagal)
        User sekr = auth.getUserByToken(tokenSekr);
        User pelajar = auth.getUserByToken(auth.login("pelajar", "pelajar123"));
        cek(kelasSvc.enroll(sekr.getId(), "sipt-abc123") == KelasService.ENROLL_OK, "Enroll sekretaris valid harus berhasil");
        cek(kelasSvc.enroll(pelajar.getId(), "SIPT-ABC123") == KelasService.ENROLL_OK, "Enroll pelajar valid harus berhasil");
        cek(kelasSvc.enroll(pelajar.getId(), "SALAH-999") == KelasService.ENROLL_NOT_FOUND, "Enroll kode invalid harus gagal");

        // 4. Uji Tugas Bersama, Isolasi Kelas, dan Progres X/Y
        Kelas k1 = admin.listKelas().get(0);
        Kelas k2 = admin.createKelas("Kelas Lain", "Tidak diikuti pelajar", 1L);
        Tugas t1 = tugasSvc.create("Tugas PBO", "Buat UML", "Matematika", LocalDate.now().plusDays(2),
                Priority.HIGH, Tugas.JenisTugas.BERSAMA, sekr.getId(), null, k1.getId(), null, "GCR", "PDF");
        tugasSvc.create("Tugas Kelas Lain", "Rahasia", "Matematika", LocalDate.now().plusDays(1),
                Priority.LOW, Tugas.JenisTugas.BERSAMA, sekr.getId(), null, k2.getId(), null, "GCR", "PDF");

        List<Tugas> visible = tugasSvc.findSharedByKelasIds(kelasSvc.findKelasIdsByUserId(pelajar.getId()));
        cek(visible.size() == 1 && visible.get(0).getId().equals(t1.getId()), "Pelajar hanya melihat tugas kelasnya");

        tugasSvc.setStatus(t1.getId(), pelajar.getId(), "SELESAI", "Sudah kumpul");
        cek(!tugasSvc.setStatus(9999L, pelajar.getId(), "SELESAI", ""), "Set status tugas yang tidak ada harus gagal");
        cek(tugasSvc.countStatus(t1.getId(), "SELESAI") == 1, "Jumlah selesai harus 1");
        cek(kelasSvc.countPelajarByClassId(k1.getId()) == 2, "Total anggota kelas (pelajar + sekretaris) harus 2");

        // 5. Uji Pengingat via Interface NotifikasiPengingat
        cek(pengingat.getUpcoming(sekr).size() == 1, "Sekretaris belum selesai -> 1 tugas upcoming");
        cek(pengingat.getUpcoming(pelajar).isEmpty(), "Pelajar sudah selesai -> 0 tugas upcoming");

        if (f.exists()) f.delete();
        System.out.println("SEMUA UJI V3 LULUS (5/5 Skenario Berhasil & Gagal)!");
    }
}
