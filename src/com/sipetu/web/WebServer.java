package com.sipetu.web;

import com.sipetu.model.*;
import com.sipetu.service.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;

public class WebServer {
    private final int port;
    private final AuthService authService;
    private final AdminService adminService;
    private final KelasService kelasService;
    private final TugasService tugasService;
    private final NotifikasiPengingat pengingatService;

    public WebServer(int port, AuthService authService, AdminService adminService,
                     KelasService kelasService, TugasService tugasService, NotifikasiPengingat pengingatService) {
        this.port = port;
        this.authService = authService;
        this.adminService = adminService;
        this.kelasService = kelasService;
        this.tugasService = tugasService;
        this.pengingatService = pengingatService;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.createContext("/", this::handle);
        server.start();
        System.out.println("=========================================");
        System.out.println("SIPETU Server berjalan di http://localhost:" + port);
        System.out.println("=========================================");
    }

    private void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod().toUpperCase();
        try {
            if (path.startsWith("/api/")) {
                handleApi(ex, method, path);
            } else {
                serveStatic(ex, path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(ex, 500, Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    private void handleApi(HttpExchange ex, String method, String path) throws IOException {
        if ("/api/auth/login".equals(path) && "POST".equals(method)) {
            Map<String, String> body = readJsonBody(ex);
            String token = authService.login(body.get("username"), body.get("password"));
            if (token == null) {
                sendJson(ex, 401, Map.of("error", "Username atau password salah"));
                return;
            }
            User u = authService.getUserByToken(token);
            ex.getResponseHeaders().add("Set-Cookie", "SIPETU_SESSION=" + token + "; Path=/; HttpOnly");
            sendJson(ex, 200, Map.of("status", "ok", "username", u.getUsername(), "role", u.getRole().name()));
            return;
        }

        if ("/api/auth/logout".equals(path) && "POST".equals(method)) {
            authService.logout(getToken(ex));
            ex.getResponseHeaders().add("Set-Cookie", "SIPETU_SESSION=; Path=/; Max-Age=0");
            sendJson(ex, 200, Map.of("status", "ok"));
            return;
        }

        User user = authService.getUserByToken(getToken(ex));
        if (user == null) {
            sendJson(ex, 401, Map.of("error", "Belum login"));
            return;
        }

        if ("/api/auth/me".equals(path) && "GET".equals(method)) {
            sendJson(ex, 200, Map.of("username", user.getUsername(), "role", user.getRole().name()));
            return;
        }

        if ("/api/enroll".equals(path) && "POST".equals(method)) {
            Map<String, String> body = readJsonBody(ex);
            int res = kelasService.enroll(user.getId(), body.get("kodeEnroll"));
            switch (res) {
                case KelasService.ENROLL_OK -> sendJson(ex, 200, Map.of("status", "ok", "message", "Berhasil bergabung ke kelas"));
                case KelasService.ENROLL_ALREADY -> sendJson(ex, 409, Map.of("error", "Sudah bergabung di kelas ini"));
                default -> sendJson(ex, 404, Map.of("error", "Kode enroll tidak valid"));
            }
            return;
        }

        if ("/api/kelas/saya".equals(path) && "GET".equals(method)) {
            List<Map<String, Object>> list = kelasService.findByUserId(user.getId()).stream()
                    .map(this::kelasToMap).toList();
            sendJson(ex, 200, list);
            return;
        }

        if ("/api/subjects".equals(path) && "GET".equals(method)) {
            List<Long> kelasIds = kelasService.findKelasIdsByUserId(user.getId());
            List<Map<String, Object>> list = adminService.listBidangStudi().stream()
                    .filter(b -> kelasIds.contains(b.getKelasId()))
                    .map(this::bidangToMap).toList();
            sendJson(ex, 200, list);
            return;
        }

        if (path.startsWith("/api/admin/")) {
            if (!user.canManageAdmin()) {
                sendJson(ex, 403, Map.of("error", "Akses ditolak: hanya Admin"));
                return;
            }
            handleAdmin(ex, method, path, user);
            return;
        }

        if (path.startsWith("/api/reminders/")) {
            if ("/api/reminders/upcoming".equals(path) && "GET".equals(method)) {
                List<Map<String, Object>> list = pengingatService.getUpcoming(user).stream()
                        .map(t -> tugasToMap(t, user)).toList();
                sendJson(ex, 200, list);
                return;
            }
            if ("/api/reminders/overdue".equals(path) && "GET".equals(method)) {
                List<Map<String, Object>> list = pengingatService.getOverdue(user).stream()
                        .map(t -> tugasToMap(t, user)).toList();
                sendJson(ex, 200, list);
                return;
            }
        }

        if (path.startsWith("/api/tasks")) {
            handleTasks(ex, method, path, user);
            return;
        }

        sendJson(ex, 404, Map.of("error", "Endpoint tidak ditemukan"));
    }

    private void handleAdmin(HttpExchange ex, String method, String path, User admin) throws IOException {
        if ("/api/admin/classes".equals(path)) {
            if ("GET".equals(method)) {
                sendJson(ex, 200, adminService.listKelas().stream().map(this::kelasToMap).toList());
                return;
            }
            if ("POST".equals(method)) {
                Map<String, String> body = readJsonBody(ex);
                Kelas k = adminService.createKelas(body.get("nama"), body.get("deskripsi"), admin.getId());
                sendJson(ex, 201, kelasToMap(k));
                return;
            }
        }

        if ("/api/admin/users".equals(path)) {
            if ("GET".equals(method)) {
                sendJson(ex, 200, adminService.listUsers().stream().map(this::userToMap).toList());
                return;
            }
            if ("POST".equals(method)) {
                Map<String, String> body = readJsonBody(ex);
                User created = adminService.createUser(body.get("username"), body.get("password"), Role.valueOf(body.get("role")));
                if (created == null) {
                    sendJson(ex, 409, Map.of("error", "Username sudah digunakan"));
                    return;
                }
                sendJson(ex, 201, userToMap(created));
                return;
            }
        }

        if (path.startsWith("/api/admin/users/")) {
            Long id = parseId(path.substring("/api/admin/users/".length()));
            if (id == null) { sendJson(ex, 400, Map.of("error", "ID tidak valid")); return; }
            if ("PUT".equals(method)) {
                Map<String, String> body = readJsonBody(ex);
                Role role = body.get("role") != null ? Role.valueOf(body.get("role")) : null;
                User u = adminService.updateUser(id, body.get("username"), role);
                if (u == null) { sendJson(ex, 404, Map.of("error", "User tidak ditemukan")); return; }
                sendJson(ex, 200, userToMap(u));
                return;
            }
            if ("DELETE".equals(method)) {
                if (adminService.deleteUser(id)) { sendEmpty(ex, 204); return; }
                sendJson(ex, 404, Map.of("error", "User tidak ditemukan"));
                return;
            }
        }

        if ("/api/admin/subjects".equals(path)) {
            if ("GET".equals(method)) {
                sendJson(ex, 200, adminService.listBidangStudi().stream().map(this::bidangToMap).toList());
                return;
            }
            if ("POST".equals(method)) {
                Map<String, String> body = readJsonBody(ex);
                Long kelasId = parseId(body.get("kelasId"));
                BidangStudi bs = adminService.createBidangStudi(body.get("nama"), body.get("kode"), kelasId);
                if (bs == null) { sendJson(ex, 400, Map.of("error", "Kelas tidak valid")); return; }
                sendJson(ex, 201, bidangToMap(bs));
                return;
            }
        }

        if (path.startsWith("/api/admin/subjects/")) {
            Long id = parseId(path.substring("/api/admin/subjects/".length()));
            if (id == null) { sendJson(ex, 400, Map.of("error", "ID tidak valid")); return; }
            if ("PUT".equals(method)) {
                Map<String, String> body = readJsonBody(ex);
                Long kelasId = body.get("kelasId") != null ? parseId(body.get("kelasId")) : null;
                BidangStudi bs = adminService.updateBidangStudi(id, body.get("nama"), body.get("kode"), kelasId);
                if (bs == null) { sendJson(ex, 404, Map.of("error", "Bidang studi tidak ditemukan")); return; }
                sendJson(ex, 200, bidangToMap(bs));
                return;
            }
            if ("DELETE".equals(method)) {
                if (adminService.deleteBidangStudi(id)) { sendEmpty(ex, 204); return; }
                sendJson(ex, 404, Map.of("error", "Bidang studi tidak ditemukan"));
                return;
            }
        }

        sendJson(ex, 404, Map.of("error", "Endpoint admin tidak ditemukan"));
    }

    private void handleTasks(HttpExchange ex, String method, String path, User user) throws IOException {
        Map<String, String> query = parseQuery(ex.getRequestURI().getRawQuery());

        // Personal tasks
        if ("/api/tasks/personal".equals(path)) {
            if ("GET".equals(method)) {
                List<Map<String, Object>> list = tugasService.findPersonalByUser(user.getId()).stream()
                        .map(t -> tugasToMap(t, user)).toList();
                sendJson(ex, 200, list);
                return;
            }
            if ("POST".equals(method)) {
                Map<String, String> body = readJsonBody(ex);
                Tugas t = tugasService.create(
                        body.get("title"),
                        body.get("deskripsi"),
                        body.get("subject"),
                        LocalDate.parse(body.get("deadline")),
                        Priority.valueOf(body.getOrDefault("priority", "MEDIUM")),
                        Tugas.JenisTugas.PRIBADI,
                        user.getId(), user.getId(), null, null,
                        body.get("tempatPengumpulan"), body.get("formatDokumen")
                );
                sendJson(ex, 201, tugasToMap(t, user));
                return;
            }
        }

        if (path.startsWith("/api/tasks/personal/")) {
            String rest = path.substring("/api/tasks/personal/".length());
            if (rest.endsWith("/status") && ("POST".equals(method) || "PATCH".equals(method))) {
                Long id = parseId(rest.substring(0, rest.length() - "/status".length()));
                Map<String, String> body = readJsonBody(ex);
                if (id != null && tugasService.setStatus(id, user.getId(), body.get("status"), body.get("keterangan"))) {
                    sendJson(ex, 200, Map.of("status", "ok"));
                } else {
                    sendJson(ex, 404, Map.of("error", "Tugas tidak ditemukan"));
                }
                return;
            }
            Long id = parseId(rest);
            if (id == null) { sendJson(ex, 400, Map.of("error", "ID tidak valid")); return; }
            if ("PUT".equals(method)) {
                Map<String, String> body = readJsonBody(ex);
                LocalDate dl = body.get("deadline") != null ? LocalDate.parse(body.get("deadline")) : null;
                Priority pr = body.get("priority") != null ? Priority.valueOf(body.get("priority")) : null;
                Tugas t = tugasService.update(id, body.get("title"), body.get("deskripsi"), body.get("subject"),
                        dl, pr, body.get("tempatPengumpulan"), body.get("formatDokumen"));
                if (t == null) { sendJson(ex, 404, Map.of("error", "Tugas tidak ditemukan")); return; }
                sendJson(ex, 200, tugasToMap(t, user));
                return;
            }
            if ("DELETE".equals(method)) {
                if (tugasService.delete(id)) { sendEmpty(ex, 204); return; }
                sendJson(ex, 404, Map.of("error", "Tugas tidak ditemukan"));
                return;
            }
        }

        // Shared tasks
        if ("/api/tasks".equals(path) || "/api/tasks/search".equals(path)) {
            if ("GET".equals(method)) {
                List<Long> kelasIds = kelasService.findKelasIdsByUserId(user.getId());
                List<Tugas> tasks = tugasService.findSharedByKelasIds(kelasIds);
                String q = query.get("q");
                String subject = query.get("subject");
                String status = query.get("status");
                String priority = query.get("priority");
                Long filterKelasId = parseId(query.get("kelasId"));
                if (filterKelasId != null) {
                    tasks = tasks.stream().filter(t -> filterKelasId.equals(t.getKelasId())).toList();
                }
                if (q != null && !q.isBlank()) {
                    String lower = q.toLowerCase();
                    tasks = tasks.stream().filter(t -> t.getTitle().toLowerCase().contains(lower)
                            || t.getSubject().toLowerCase().contains(lower)).toList();
                }
                if (subject != null && !subject.isBlank()) {
                    tasks = tasks.stream().filter(t -> subject.equalsIgnoreCase(t.getSubject())).toList();
                }
                if (priority != null && !priority.isBlank()) {
                    tasks = tasks.stream().filter(t -> priority.equalsIgnoreCase(t.getPriority().name())).toList();
                }
                if (status != null && !status.isBlank()) {
                    tasks = tasks.stream().filter(t -> status.equalsIgnoreCase(tugasService.getStudentStatus(t.getId(), user.getId()))).toList();
                }
                List<Map<String, Object>> result = tasks.stream().map(t -> tugasToMap(t, user)).toList();
                sendJson(ex, 200, result);
                return;
            }
            if ("POST".equals(method) && "/api/tasks".equals(path)) {
                if (!user.canManageSharedTasks()) {
                    sendJson(ex, 403, Map.of("error", "Akses ditolak: hanya Sekretaris atau Admin"));
                    return;
                }
                Map<String, String> body = readJsonBody(ex);
                Long kelasId = parseId(body.get("kelasId"));
                if (kelasId == null) {
                    List<Long> kelasIds = kelasService.findKelasIdsByUserId(user.getId());
                    if (!kelasIds.isEmpty()) kelasId = kelasIds.get(0);
                }
                if (kelasId == null) {
                    sendJson(ex, 400, Map.of("error", "Anda belum enroll ke kelas manapun"));
                    return;
                }
                Tugas t = tugasService.create(
                        body.get("title"),
                        body.get("deskripsi"),
                        body.get("subject"),
                        LocalDate.parse(body.get("deadline")),
                        Priority.valueOf(body.getOrDefault("priority", "MEDIUM")),
                        Tugas.JenisTugas.BERSAMA,
                        user.getId(), null, kelasId,
                        parseId(body.get("bidangStudiId")),
                        body.get("tempatPengumpulan"),
                        body.get("formatDokumen")
                );
                sendJson(ex, 201, tugasToMap(t, user));
                return;
            }
        }

        if (path.startsWith("/api/tasks/")) {
            String rest = path.substring("/api/tasks/".length());
            if (rest.endsWith("/status") && ("POST".equals(method) || "PATCH".equals(method))) {
                Long id = parseId(rest.substring(0, rest.length() - "/status".length()));
                Map<String, String> body = readJsonBody(ex);
                if (id != null && tugasService.setStatus(id, user.getId(), body.get("status"), body.get("keterangan"))) {
                    sendJson(ex, 200, Map.of("status", "ok"));
                } else {
                    sendJson(ex, 404, Map.of("error", "Tugas tidak ditemukan"));
                }
                return;
            }
            if (rest.endsWith("/stats") && "GET".equals(method)) {
                Long id = parseId(rest.substring(0, rest.length() - "/stats".length()));
                Tugas t = id != null ? tugasService.findById(id) : null;
                if (t == null) { sendJson(ex, 404, Map.of("error", "Tugas tidak ditemukan")); return; }
                long selesai = tugasService.countStatus(id, "SELESAI");
                long total = kelasService.countPelajarByClassId(t.getKelasId());
                sendJson(ex, 200, Map.of("taskId", id, "selesai", selesai, "belum", Math.max(0, total - selesai), "totalPelajar", total));
                return;
            }
            Long id = parseId(rest);
            if (id == null) { sendJson(ex, 400, Map.of("error", "ID tidak valid")); return; }
            if ("PUT".equals(method)) {
                if (!user.canManageSharedTasks()) { sendJson(ex, 403, Map.of("error", "Akses ditolak")); return; }
                Map<String, String> body = readJsonBody(ex);
                LocalDate dl = body.get("deadline") != null ? LocalDate.parse(body.get("deadline")) : null;
                Priority pr = body.get("priority") != null ? Priority.valueOf(body.get("priority")) : null;
                Tugas t = tugasService.update(id, body.get("title"), body.get("deskripsi"), body.get("subject"),
                        dl, pr, body.get("tempatPengumpulan"), body.get("formatDokumen"));
                if (t == null) { sendJson(ex, 404, Map.of("error", "Tugas tidak ditemukan")); return; }
                sendJson(ex, 200, tugasToMap(t, user));
                return;
            }
            if ("DELETE".equals(method)) {
                if (!user.canManageSharedTasks()) { sendJson(ex, 403, Map.of("error", "Akses ditolak")); return; }
                if (tugasService.delete(id)) { sendEmpty(ex, 204); return; }
                sendJson(ex, 404, Map.of("error", "Tugas tidak ditemukan"));
                return;
            }
        }

        sendJson(ex, 404, Map.of("error", "Endpoint tugas tidak ditemukan"));
    }

    private Map<String, Object> kelasToMap(Kelas k) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", k.getId());
        m.put("nama", k.getNama());
        m.put("deskripsi", k.getDeskripsi());
        m.put("kodeEnroll", k.getKodeEnroll());
        return m;
    }

    private Map<String, Object> userToMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("role", u.getRole().name());
        return m;
    }

    private Map<String, Object> bidangToMap(BidangStudi b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("nama", b.getNama());
        m.put("kode", b.getKode());
        m.put("kelasId", b.getKelasId());
        return m;
    }

    private Map<String, Object> tugasToMap(Tugas t, User viewer) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("title", t.getTitle());
        m.put("deskripsi", t.getDeskripsi());
        m.put("subject", t.getSubject());
        m.put("deadline", t.getDeadline().toString());
        m.put("priority", t.getPriority().name());
        m.put("jenis", t.getJenis().name());
        m.put("kelasId", t.getKelasId());
        String kelasNama = "-";
        if (t.getKelasId() != null) {
            for (Kelas k : adminService.listKelas()) {
                if (k.getId().equals(t.getKelasId())) { kelasNama = k.getNama(); break; }
            }
        }
        m.put("kelasNama", kelasNama);
        m.put("tempatPengumpulan", t.getTempatPengumpulan());
        m.put("formatDokumen", t.getFormatDokumen());
        m.put("daysUntilDeadline", t.daysUntilDeadline());
        m.put("overdue", t.isOverdue());
        m.put("myStatus", tugasService.getStudentStatus(t.getId(), viewer.getId()));
        m.put("selesaiCount", tugasService.countStatus(t.getId(), "SELESAI"));
        m.put("totalPelajar", t.getKelasId() != null ? kelasService.countPelajarByClassId(t.getKelasId()) : 0);
        return m;
    }

    private void serveStatic(HttpExchange ex, String path) throws IOException {
        if ("/".equals(path)) path = "/index.html";
        Path filePath = Path.of("web", path.substring(1)).normalize();
        if (!filePath.startsWith("web") || !Files.exists(filePath) || Files.isDirectory(filePath)) {
            byte[] notFound = "Not Found".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, notFound.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(notFound); }
            return;
        }
        String ct = "text/plain; charset=utf-8";
        if (path.endsWith(".html")) ct = "text/html; charset=utf-8";
        else if (path.endsWith(".css")) ct = "text/css; charset=utf-8";
        else if (path.endsWith(".js")) ct = "application/javascript; charset=utf-8";
        byte[] bytes = Files.readAllBytes(filePath);
        ex.getResponseHeaders().set("Content-Type", ct);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private Map<String, String> readJsonBody(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return JsonUtil.parseObject(body);
    }

    private String getToken(HttpExchange ex) {
        List<String> cookies = ex.getRequestHeaders().get("Cookie");
        if (cookies == null) return null;
        for (String header : cookies) {
            for (String part : header.split(";")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2 && "SIPETU_SESSION".equals(kv[0])) {
                    return kv[1];
                }
            }
        }
        return null;
    }

    private Long parseId(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isBlank()) return map;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private void sendJson(HttpExchange ex, int status, Object data) throws IOException {
        byte[] bytes = JsonUtil.toJson(data).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private void sendEmpty(HttpExchange ex, int status) throws IOException {
        ex.sendResponseHeaders(status, -1);
        ex.close();
    }
}
