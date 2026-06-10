package com.telemed.web.controller;

import com.telemed.common.result.Result;
import com.telemed.model.entity.Department;
import com.telemed.service.DepartmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/department")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/hospital/{hospitalId}")
    public Result<List<Department>> getByHospital(@PathVariable Long hospitalId) {
        List<Department> list = departmentService.getActiveDepartmentsByHospital(hospitalId);
        return Result.ok(list);
    }

    @GetMapping("/campus/{campusId}")
    public Result<List<Department>> getByCampus(@PathVariable Long campusId) {
        List<Department> list = departmentService.getActiveDepartmentsByCampus(campusId);
        return Result.ok(list);
    }
}
