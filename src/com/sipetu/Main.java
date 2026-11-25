package com.sipetu;

import com.sipetu.repository.DataStore;
import com.sipetu.service.*;
import com.sipetu.web.WebServer;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        DataStore store = DataStore.loadOrCreate();
        AuthService authService = new AuthService(store);
        AdminService adminService = new AdminService(store);
        KelasService kelasService = new KelasService(store);
        TugasService tugasService = new TugasService(store);
        NotifikasiPengingat pengingatService = new PengingatService(tugasService, kelasService);

        WebServer server = new WebServer(port, authService, adminService, kelasService, tugasService, pengingatService);
        server.start();
    }
}
