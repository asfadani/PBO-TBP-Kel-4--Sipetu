# SIPETU — Implementasi V3 (Java 17 PBO, Tanpa Docker Compose)

Aplikasi web manajemen dan pengingat tugas kelas berbasis **Java 17 murni** (`com.sun.net.httpserver.HttpServer`) dengan arsitektur PBO 4 lapisan (`model`, `repository`, `service`, `web`) dan penyimpanan persisten lokal (`data/sipetu.dat`).

## Cara Menjalankan Aplikasi

```bash
chmod +x run.sh
./run.sh
```

Atau di Windows:

```bat
run.bat
```

Buka browser di `http://localhost:8080`.

## Cara Menjalankan Kode Uji V3

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out com.sipetu.test.SipetuTest
```

## Akun Demo & Kode Kelas Default

- **Admin**: `admin` / `admin123`
- **Sekretaris**: `sekretaris` / `sekretaris123`
- **Pelajar**: `pelajar` / `pelajar123`
- **Kode Kelas Default**: `SIPT-ABC123`
