package com.telemed.model.repository;

import com.telemed.model.entity.FaceVerifyCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FaceVerifyCounterRepository extends JpaRepository<FaceVerifyCounter, Long> {

    Optional<FaceVerifyCounter> findByPatientId(Long patientId);

    @Modifying
    @Query("UPDATE FaceVerifyCounter c SET c.failureCount = c.failureCount + 1 WHERE c.patientId = :patientId")
    int incrementFailureCount(@Param("patientId") Long patientId);

    @Modifying
    @Query("UPDATE FaceVerifyCounter c SET c.failureCount = 0 WHERE c.patientId = :patientId")
    int resetFailureCount(@Param("patientId") Long patientId);

    @Modifying
    @Query("UPDATE FaceVerifyCounter c SET c.locked = 1, c.lockTime = CURRENT_TIMESTAMP WHERE c.patientId = :patientId")
    int lockPatient(@Param("patientId") Long patientId);
}
