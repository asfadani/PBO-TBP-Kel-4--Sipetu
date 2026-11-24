package com.sipetu.web;

import com.sipetu.service.*;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Kerangka lapisan Presentation (WebServer) berbasis HttpServer bawaan JDK 17.
 */
public class WebServer {
    private final int port;
    private final AuthService authService;
    private final AdminService adminService;
    private final KelasService kelasService;
    private final TugasService tugasService;
    private final PengingatService pengingatService;

    public WebServer(int port, AuthService authService, AdminService adminService,
                     KelasService kelasService, TugasService tugasService, PengingatService pengingatService) {
        this.port = port;
        this.authService = authService;
        this.adminService = adminService;
        this.kelasService = kelasService;
        this.tugasService = tugasService;
        this.pengingatService = pengingatService;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/health", ex -> {
            byte[] ok = "{\"status\":\"ok\"}".getBytes();
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(200, ok.length);
            ex.getResponseBody().write(ok);
            ex.close();
        });
        server.start();
        System.out.println("WebServer V2 siap di port " + port);
    }
}
