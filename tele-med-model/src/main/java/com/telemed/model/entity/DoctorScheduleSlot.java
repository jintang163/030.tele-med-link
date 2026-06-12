package com.telemed.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_doctor_schedule_slot", indexes = {
        @Index(name = "idx_doctor_date_slot", columnList = "doctorId, scheduleDate, slotTime", unique = true),
        @Index(name = "idx_schedule_date", columnList = "scheduleDate"),
        @Index(name = "idx_doctor_date", columnList = "doctorId, scheduleDate")
})
public class DoctorScheduleSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long doctorId;

    @Column(nullable = false)
    private LocalDate scheduleDate;

    @Column(nullable = false, length = 5)
    private String slotTime;

    @Column(nullable = false)
    private Integer slotIndex;

    @Column(nullable = false)
    private Integer maxPatients = 1;

    @Column(nullable = false)
    private Integer bookedCount = 0;

    @Column(nullable = false)
    private Integer status = 0;

    @Column(length = 500)
    private String suspendReason;

    private Long shiftToDoctorId;

    private Long shiftToSlotId;

    private Long operatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
