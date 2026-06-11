package com.telemed.model.repository;

import com.telemed.model.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByStatus(Integer status);

    List<Consultation> findByDoctorIdAndStatus(Long doctorId, Integer status);

    List<Consultation> findByDoctorId(Long doctorId);

    List<Consultation> findByPatientIdOrderByCreateTimeDesc(Long patientId);

    Optional<Consultation> findByConsultationNo(String consultationNo);

    List<Consultation> findByCrossCampusTrue();

    List<Consultation> findByCrossCampusTrueAndStatus(Integer status);

    List<Consultation> findBySourceCampusId(Long sourceCampusId);

    List<Consultation> findByTargetCampusId(Long targetCampusId);

    List<Consultation> findByTargetCampusIdAndStatus(Long targetCampusId, Integer status);

    List<Consultation> findByCampusTag(String campusTag);

    @Query("SELECT c FROM Consultation c WHERE c.status = :status AND c.expireTime < :now AND c.crossCampus = true")
    List<Consultation> findExpiredCrossCampusConsultations(@Param("status") Integer status, @Param("now") LocalDateTime now);

    @Query("SELECT c FROM Consultation c WHERE c.doctorId = :doctorId AND c.status IN :statuses AND c.crossCampus = true")
    List<Consultation> findCrossCampusByDoctorIdAndStatuses(@Param("doctorId") Long doctorId, @Param("statuses") List<Integer> statuses);
}
