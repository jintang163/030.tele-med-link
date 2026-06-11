package com.telemed.model.repository;

import com.telemed.model.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserId(Long userId);

    List<Doctor> findByHospitalId(Long hospitalId);

    List<Doctor> findByDepartmentAndHospitalId(String department, Long hospitalId);

    List<Doctor> findByCampusId(Long campusId);

    List<Doctor> findByCampusIdAndStatus(Long campusId, Integer status);

    List<Doctor> findByHospitalIdAndCampusId(Long hospitalId, Long campusId);

    @Query("SELECT d FROM Doctor d WHERE d.department = :department AND d.campusId = :campusId AND d.status = :status")
    List<Doctor> findByDepartmentAndCampusIdAndStatus(@Param("department") String department,
                                                        @Param("campusId") Long campusId,
                                                        @Param("status") Integer status);

    @Query("SELECT d FROM Doctor d WHERE d.hospitalId = :hospitalId AND d.campusId <> :excludeCampusId AND d.status = 1")
    List<Doctor> findOtherCampusDoctors(@Param("hospitalId") Long hospitalId, @Param("excludeCampusId") Long excludeCampusId);
}
