package com.telemed.model.repository;

import com.telemed.model.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserId(Long userId);

    List<Doctor> findByHospitalId(Long hospitalId);

    List<Doctor> findByDepartmentAndHospitalId(String department, Long hospitalId);
}
