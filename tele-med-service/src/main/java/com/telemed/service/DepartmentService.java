package com.telemed.service;

import com.telemed.model.entity.Department;

import java.util.List;

public interface DepartmentService {

    Department createDepartment(Department department);

    Department updateDepartment(Department department);

    void deleteDepartment(Long id);

    Department getDepartmentById(Long id);

    List<Department> getDepartmentsByHospital(Long hospitalId);

    List<Department> getDepartmentsByCampus(Long campusId);

    List<Department> getActiveDepartmentsByHospital(Long hospitalId);

    List<Department> getActiveDepartmentsByCampus(Long campusId);
}
