# SIPETU — Rancangan & Kerangka Kode V2 (Java 17 PBO)

Tahap **V2** berisi kerangka kode Java 17 terstruktur yang **dapat dikompilasi dan disimulasikan** sebelum implementasi penuh fitur pada tahap **V3** (kolaborasi GitHub branch/PR per fitur).

## Arsitektur Berlapis (4 Layer, Tanpa Docker Compose)

1. `com.sipetu.model` — Entitas domain & aturan dasar (`User` abstract, `Admin`, `Sekretaris`, `Pelajar`, `Role`, `Kelas`, `EnrollmentKelas`, `BidangStudi`, `Tugas`, `Priority`, `DetailTugas`).
2. `com.sipetu.repository` — Kontrak abstraksi `Repository<T, ID>` dan penyimpanan persisten lokal `DataStore` (`data/sipetu.dat`).
3. `com.sipetu.service` — Layanan logika bisnis (`AuthService`, `AdminService`, `KelasService`, `TugasService`, `PengingatService`) dengan *constructor injection*.
4. `com.sipetu.web` — Lapisan antarmuka web REST API (`WebServer`, `JsonUtil`).
5. `com.sipetu.Main` — *Composition Root* untuk merakit objek dan menjalankan simulasi alur V2.

## Cara Menjalankan Aplikasi Web (V3)

```bash
chmod +x run.sh
./run.sh
# Buka http://localhost:8080
```
