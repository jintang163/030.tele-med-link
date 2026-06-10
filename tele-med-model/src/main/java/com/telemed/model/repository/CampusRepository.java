package com.telemed.model.repository;

import com.telemed.model.entity.Campus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampusRepository extends JpaRepository<Campus, Long> {

    List<Campus> findByHospitalId(Long hospitalId);

    List<Campus> findByHospitalIdAndStatus(Long hospitalId, Integer status);
}
