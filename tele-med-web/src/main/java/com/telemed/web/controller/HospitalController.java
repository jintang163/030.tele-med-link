package com.telemed.web.controller;

import com.telemed.common.result.Result;
import com.telemed.model.entity.Campus;
import com.telemed.model.entity.Doctor;
import com.telemed.model.entity.Hospital;
import com.telemed.model.repository.DoctorRepository;
import com.telemed.service.HospitalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospital")
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DoctorRepository doctorRepository;

    @GetMapping("/list")
    public Result<List<Hospital>> list() {
        List<Hospital> list = hospitalService.getAllHospitals();
        return Result.ok(list);
    }

    @GetMapping("/{id}")
    public Result<Hospital> getById(@PathVariable Long id) {
        Hospital hospital = hospitalService.getHospitalById(id);
        return Result.ok(hospital);
    }

    @GetMapping("/{hospitalId}/campuses")
    public Result<List<Campus>> getCampuses(@PathVariable Long hospitalId) {
        List<Campus> list = hospitalService.getCampusesByHospitalId(hospitalId);
        return Result.ok(list);
    }

    @GetMapping("/campus/{id}")
    public Result<Campus> getCampusById(@PathVariable Long id) {
        Campus campus = hospitalService.getCampusById(id);
        return Result.ok(campus);
    }

    @GetMapping("/doctors")
    public Result<List<Doctor>> getDoctors(
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) String department) {
        List<Doctor> doctors;
        if (hospitalId != null) {
            doctors = doctorRepository.findByHospitalId(hospitalId);
        } else {
            doctors = doctorRepository.findAll();
        }
        return Result.ok(doctors);
    }

    @GetMapping("/doctor/{id}")
    public Result<Doctor> getDoctorById(@PathVariable Long id) {
        return doctorRepository.findById(id)
                .map(Result::ok)
                .orElse(Result.fail("医生不存在"));
    }
}
