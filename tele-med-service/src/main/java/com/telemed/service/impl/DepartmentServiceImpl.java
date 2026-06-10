package com.telemed.service.impl;

import com.telemed.common.context.UserContext;
import com.telemed.common.exception.BusinessException;
import com.telemed.model.entity.Department;
import com.telemed.model.repository.DepartmentRepository;
import com.telemed.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    public Department createDepartment(Department department) {
        if (department.getHospitalId() == null) {
            department.setHospitalId(UserContext.getHospitalId());
        }
        return departmentRepository.save(department);
    }

    @Override
    public Department updateDepartment(Department department) {
        Department existing = departmentRepository.findById(department.getId())
                .orElseThrow(() -> new BusinessException("科室不存在"));
        if (department.getName() != null) {
            existing.setName(department.getName());
        }
        if (department.getDescription() != null) {
            existing.setDescription(department.getDescription());
        }
        if (department.getSortOrder() != null) {
            existing.setSortOrder(department.getSortOrder());
        }
        if (department.getStatus() != null) {
            existing.setStatus(department.getStatus());
        }
        return departmentRepository.save(existing);
    }

    @Override
    public void deleteDepartment(Long id) {
        departmentRepository.deleteById(id);
    }

    @Override
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("科室不存在"));
    }

    @Override
    public List<Department> getDepartmentsByHospital(Long hospitalId) {
        return departmentRepository.findByHospitalIdOrderBySortOrderAsc(hospitalId);
    }

    @Override
    public List<Department> getDepartmentsByCampus(Long campusId) {
        return departmentRepository.findByCampusIdOrderBySortOrderAsc(campusId);
    }

    @Override
    public List<Department> getActiveDepartmentsByHospital(Long hospitalId) {
        return departmentRepository.findByHospitalIdAndStatusOrderBySortOrderAsc(hospitalId, 1);
    }

    @Override
    public List<Department> getActiveDepartmentsByCampus(Long campusId) {
        return departmentRepository.findByCampusIdAndStatusOrderBySortOrderAsc(campusId, 1);
    }
}
