package com.sipetu.model;

import java.io.Serializable;

public class EnrollmentKelas implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long classId;
    private Long userId;

    public EnrollmentKelas(Long id, Long classId, Long userId) {
        this.id = id;
        this.classId = classId;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
