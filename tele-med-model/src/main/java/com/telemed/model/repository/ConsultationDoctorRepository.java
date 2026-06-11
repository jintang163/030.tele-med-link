package com.telemed.model.repository;

import com.telemed.model.entity.ConsultationDoctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationDoctorRepository extends JpaRepository<ConsultationDoctor, Long> {

    List<ConsultationDoctor> findByConsultationId(Long consultationId);

    List<ConsultationDoctor> findByConsultationIdAndRoleType(Long consultationId, Integer roleType);

    List<ConsultationDoctor> findByDoctorIdAndJoinStatus(Long doctorId, Integer joinStatus);

    Optional<ConsultationDoctor> findByConsultationIdAndDoctorId(Long consultationId, Long doctorId);

    List<ConsultationDoctor> findByDoctorId(Long doctorId);

    void deleteByConsultationId(Long consultationId);
}
