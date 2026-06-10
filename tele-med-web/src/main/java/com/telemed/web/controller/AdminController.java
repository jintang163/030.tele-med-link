package com.telemed.web.controller;

import com.telemed.common.result.Result;
import com.telemed.common.vo.CampusStatsVO;
import com.telemed.model.entity.Campus;
import com.telemed.model.entity.Department;
import com.telemed.model.entity.Hospital;
import com.telemed.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final HospitalService hospitalService;
    private final DepartmentService departmentService;
    private final DoctorOnlineService doctorOnlineService;
    private final ConsultationService consultationService;
    private final AppointmentService appointmentService;

    public AdminController(HospitalService hospitalService,
                           DepartmentService departmentService,
                           DoctorOnlineService doctorOnlineService,
                           ConsultationService consultationService,
                           AppointmentService appointmentService) {
        this.hospitalService = hospitalService;
        this.departmentService = departmentService;
        this.doctorOnlineService = doctorOnlineService;
        this.consultationService = consultationService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> overview = new HashMap<>();
        List<Hospital> hospitals = hospitalService.getAllHospitals();
        int totalCampuses = 0;
        for (Hospital h : hospitals) {
            List<Campus> campuses = hospitalService.getCampusesByHospital(h.getId());
            totalCampuses += campuses.size();
        }
        overview.put("hospitalCount", hospitals.size());
        overview.put("campusCount", totalCampuses);
        overview.put("onlineDoctorCount", doctorOnlineService.getOnlineCount());
        return Result.ok(overview);
    }

    @GetMapping("/campus-stats")
    public Result<List<CampusStatsVO>> getCampusStats(@RequestParam(required = false) Long hospitalId) {
        List<CampusStatsVO> result = new ArrayList<>();
        List<Hospital> hospitals;
        if (hospitalId != null) {
            Hospital hospital = hospitalService.getHospitalById(hospitalId);
            hospitals = List.of(hospital);
        } else {
            hospitals = hospitalService.getAllHospitals();
        }

        for (Hospital hospital : hospitals) {
            List<Campus> campuses = hospitalService.getCampusesByHospital(hospital.getId());
            for (Campus campus : campuses) {
                List<Long> onlineDoctors = doctorOnlineService.getOnlineDoctorIdsByCampus(campus.getId());
                CampusStatsVO stats = CampusStatsVO.builder()
                        .campusId(campus.getId())
                        .campusName(campus.getName())
                        .hospitalId(hospital.getId())
                        .hospitalName(hospital.getName())
                        .onlineDoctorCount(onlineDoctors.size())
                        .doctorCount(0)
                        .patientCount(0)
                        .todayConsultationCount(0L)
                        .totalConsultationCount(0L)
                        .todayAppointmentCount(0L)
                        .totalAppointmentCount(0L)
                        .build();
                result.add(stats);
            }
        }
        return Result.ok(result);
    }

    @PostMapping("/hospital")
    public Result<Hospital> createHospital(@RequestBody Hospital hospital) {
        Hospital saved = hospitalService.createHospital(hospital);
        return Result.ok(saved);
    }

    @PutMapping("/hospital")
    public Result<Hospital> updateHospital(@RequestBody Hospital hospital) {
        Hospital updated = hospitalService.updateHospital(hospital);
        return Result.ok(updated);
    }

    @DeleteMapping("/hospital/{id}")
    public Result<Void> deleteHospital(@PathVariable Long id) {
        hospitalService.deleteHospital(id);
        return Result.ok();
    }

    @PostMapping("/campus")
    public Result<Campus> createCampus(@RequestBody Campus campus) {
        Campus saved = hospitalService.createCampus(campus);
        return Result.ok(saved);
    }

    @PutMapping("/campus")
    public Result<Campus> updateCampus(@RequestBody Campus campus) {
        Campus updated = hospitalService.updateCampus(campus);
        return Result.ok(updated);
    }

    @DeleteMapping("/campus/{id}")
    public Result<Void> deleteCampus(@PathVariable Long id) {
        hospitalService.deleteCampus(id);
        return Result.ok();
    }

    @PostMapping("/department")
    public Result<Department> createDepartment(@RequestBody Department department) {
        Department saved = departmentService.createDepartment(department);
        return Result.ok(saved);
    }

    @PutMapping("/department")
    public Result<Department> updateDepartment(@RequestBody Department department) {
        Department updated = departmentService.updateDepartment(department);
        return Result.ok(updated);
    }

    @DeleteMapping("/department/{id}")
    public Result<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return Result.ok();
    }
}
