package com.telemed.model.repository;

import com.telemed.model.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByHospitalIdOrderBySortOrderAsc(Long hospitalId);

    List<Department> findByCampusIdOrderBySortOrderAsc(Long campusId);

    List<Department> findByHospitalIdAndStatusOrderBySortOrderAsc(Long hospitalId, Integer status);

    List<Department> findByCampusIdAndStatusOrderBySortOrderAsc(Long campusId, Integer status);
}
