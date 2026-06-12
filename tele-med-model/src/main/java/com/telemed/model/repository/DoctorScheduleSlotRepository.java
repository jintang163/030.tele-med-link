package com.telemed.model.repository;

import com.telemed.model.entity.DoctorScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DoctorScheduleSlotRepository extends JpaRepository<DoctorScheduleSlot, Long> {

    List<DoctorScheduleSlot> findByDoctorIdAndScheduleDateBetweenOrderByScheduleDateAscSlotIndexAsc(
            Long doctorId, LocalDate startDate, LocalDate endDate);

    List<DoctorScheduleSlot> findByDoctorIdAndScheduleDateOrderBySlotIndexAsc(Long doctorId, LocalDate scheduleDate);

    @Query("SELECT s FROM DoctorScheduleSlot s WHERE s.scheduleDate BETWEEN :startDate AND :endDate AND s.status <> :status")
    List<DoctorScheduleSlot> findByScheduleDateBetweenAndStatusNot(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") Integer status);

    Optional<DoctorScheduleSlot> findByIdAndDoctorId(Long id, Long doctorId);

    long countByDoctorIdAndScheduleDateAndSlotTime(Long doctorId, LocalDate scheduleDate, String slotTime);

    @Query("SELECT s FROM DoctorScheduleSlot s WHERE s.scheduleDate = :date AND s.bookedCount > 0 ORDER BY s.doctorId, s.slotIndex")
    List<DoctorScheduleSlot> findByScheduleDateAndBookedCountGreaterThan(@Param("date") LocalDate date);

    @Modifying
    @Query("UPDATE DoctorScheduleSlot s SET s.bookedCount = s.bookedCount + 1 WHERE s.id = :id AND s.bookedCount < s.maxPatients AND s.status = 0")
    int incrementBookedCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE DoctorScheduleSlot s SET s.bookedCount = s.bookedCount - 1 WHERE s.id = :id AND s.bookedCount > 0")
    int decrementBookedCount(@Param("id") Long id);

    @Query("SELECT s FROM DoctorScheduleSlot s WHERE s.doctorId = :doctorId AND s.scheduleDate > :today AND s.status = 0 AND (s.maxPatients - s.bookedCount) > 0 ORDER BY s.scheduleDate, s.slotIndex")
    List<DoctorScheduleSlot> findFutureAvailableSlotsByDoctor(
            @Param("doctorId") Long doctorId,
            @Param("today") LocalDate today);
}
