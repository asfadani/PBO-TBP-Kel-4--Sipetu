package com.sipetu;

import com.sipetu.model.*;
import com.sipetu.repository.DataStore;
import com.sipetu.service.*;
import com.sipetu.web.WebServer;

import java.time.LocalDate;

/**
 * Composition Root V2: merakit seluruh lapisan (Repository -> Service -> Presentation)
 * dan menjalankan simulasi alur dasar untuk memverifikasi rancangan V2.
 */
public class Main {
    public static void main(String[] args) {
        // 1. Inisialisasi DataStore (persistensi file lokal sipetu.dat)
        DataStore store = DataStore.loadOrCreate();

        // 2. Constructor Injection ke lapisan Service
        AuthService authService = new AuthService(store);
        AdminService adminService = new AdminService(store);
        KelasService kelasService = new KelasService(store);
        TugasService tugasService = new TugasService(store);
        PengingatService pengingatService = new PengingatService(tugasService, kelasService);

        // 3. Simulasi alur V2 (Admin -> Enroll Sekretaris & Pelajar -> Upload Tugas -> Selesai)
        String tokenAdmin = authService.login("admin", "admin123");
        User admin = authService.getUserByToken(tokenAdmin);
        Kelas kelas = adminService.listKelas().get(0);

        String tokenSekr = authService.login("sekretaris", "sekretaris123");
        User sekr = authService.getUserByToken(tokenSekr);
        kelasService.enroll(sekr.getId(), kelas.getKodeEnroll());

        String tokenPelajar = authService.login("pelajar", "pelajar123");
        User pelajar = authService.getUserByToken(tokenPelajar);
        kelasService.enroll(pelajar.getId(), kelas.getKodeEnroll());

        Tugas tugas = tugasService.create(
                "Simulasi Tugas V2", "Implementasi kerangka kelas", "Matematika",
                LocalDate.now().plusDays(2), Priority.HIGH, Tugas.JenisTugas.BERSAMA,
                sekr.getId(), null, kelas.getId(), null, "Google Classroom", "PDF"
        );
        tugasService.setStatus(tugas.getId(), pelajar.getId(), "SELESAI", "Selesai tepat waktu");

        long selesai = tugasService.countStatus(tugas.getId(), "SELESAI");
        long total = kelasService.countPelajarByClassId(kelas.getId());

        System.out.println("=== SIMULASI V2 SIPETU ===");
        System.out.println("Kelas         : " + kelas.getNama() + " (" + kelas.getKodeEnroll() + ")");
        System.out.println("Tugas         : " + tugas.getTitle() + " [" + tugas.getPriority() + "]");
        System.out.println("Progres Kelas : " + selesai + "/" + total + " (termasuk sekretaris)");
        System.out.println("Pengingat <=3h: " + pengingatService.getUpcoming(sekr).size() + " tugas");
        System.out.println("BUILD & SIMULASI V2 BERHASIL!");

        // Kerangka WebServer siap diaktifkan pada V3:
        WebServer webServer = new WebServer(8080, authService, adminService, kelasService, tugasService, pengingatService);
    }
}
