package com.telemed.model.repository;

import com.telemed.model.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUserId(Long userId);

    Optional<Patient> findByOpenId(String openId);

    List<Patient> findByHospitalId(Long hospitalId);
}
