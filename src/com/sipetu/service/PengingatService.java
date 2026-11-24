package com.sipetu.service;

import com.sipetu.model.Tugas;
import com.sipetu.model.User;

import java.util.ArrayList;
import java.util.List;

public class PengingatService implements NotifikasiPengingat {
    private final TugasService tugasService;
    private final KelasService kelasService;

    public PengingatService(TugasService tugasService, KelasService kelasService) {
        this.tugasService = tugasService;
        this.kelasService = kelasService;
    }

    private List<Tugas> getAccessibleTasks(User user) {
        List<Tugas> all = new ArrayList<>();
        List<Long> kelasIds = kelasService.findKelasIdsByUserId(user.getId());
        all.addAll(tugasService.findSharedByKelasIds(kelasIds));
        all.addAll(tugasService.findPersonalByUser(user.getId()));
        return all;
    }

    public List<Tugas> getUpcoming(User user) {
        List<Tugas> result = new ArrayList<>();
        for (Tugas t : getAccessibleTasks(user)) {
            String status = tugasService.getStudentStatus(t.getId(), user.getId());
            long days = t.daysUntilDeadline();
            if (!"SELESAI".equals(status) && days >= 0 && days <= 3) {
                result.add(t);
            }
        }
        return result;
    }

    public List<Tugas> getOverdue(User user) {
        List<Tugas> result = new ArrayList<>();
        for (Tugas t : getAccessibleTasks(user)) {
            String status = tugasService.getStudentStatus(t.getId(), user.getId());
            if (!"SELESAI".equals(status) && t.daysUntilDeadline() < 0) {
                result.add(t);
            }
        }
        return result;
    }
}
