package com.sipetu.service;

import com.sipetu.model.Tugas;
import com.sipetu.model.User;

import java.util.List;

/**
 * Interface kontrak untuk layanan pengingat tenggat tugas (Polymorphism & DIP).
 */
public interface NotifikasiPengingat {
    List<Tugas> getUpcoming(User user);
    List<Tugas> getOverdue(User user);
}
