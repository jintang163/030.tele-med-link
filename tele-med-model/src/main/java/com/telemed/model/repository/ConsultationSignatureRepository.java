package com.telemed.model.repository;

import com.telemed.model.entity.ConsultationSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationSignatureRepository extends JpaRepository<ConsultationSignature, Long> {

    List<ConsultationSignature> findByConsultationIdOrderBySignOrderAsc(Long consultationId);

    Optional<ConsultationSignature> findByConsultationIdAndDoctorId(Long consultationId, Long doctorId);

    List<ConsultationSignature> findByConsultationIdAndSignStatus(Long consultationId, Integer signStatus);

    long countByConsultationIdAndSignStatus(Long consultationId, Integer signStatus);
}
