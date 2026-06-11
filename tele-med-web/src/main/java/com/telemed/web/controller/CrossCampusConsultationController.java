package com.telemed.web.controller;

import com.telemed.common.dto.CrossCampusConsultationCreateDTO;
import com.telemed.common.result.Result;
import com.telemed.common.vo.ConsultationVO;
import com.telemed.common.vo.DoctorScheduleVO;
import com.telemed.common.vo.DoctorVO;
import com.telemed.common.vo.TimeSlotVO;
import com.telemed.model.entity.Campus;
import com.telemed.model.entity.Consultation;
import com.telemed.model.entity.Doctor;
import com.telemed.model.entity.Hospital;
import com.telemed.model.entity.User;
import com.telemed.model.repository.CampusRepository;
import com.telemed.model.repository.DoctorRepository;
import com.telemed.model.repository.HospitalRepository;
import com.telemed.model.repository.UserRepository;
import com.telemed.service.CrossCampusConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cross-campus")
@RequiredArgsConstructor
public class CrossCampusConsultationController {

    private final CrossCampusConsultationService crossCampusService;
    private final DoctorRepository doctorRepository;
    private final CampusRepository campusRepository;
    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;

    @PostMapping("/consultation/create")
    public Result<Consultation> create(@RequestBody CrossCampusConsultationCreateDTO dto) {
        Consultation consultation = crossCampusService.createCrossCampusConsultation(dto);
        return Result.ok(consultation);
    }

    @PostMapping("/consultation/confirm")
    public Result<Consultation> confirm(@RequestParam Long consultationId, @RequestParam Long doctorId) {
        Consultation consultation = crossCampusService.confirmCrossCampusConsultation(consultationId, doctorId);
        return Result.ok(consultation);
    }

    @PostMapping("/consultation/reject")
    public Result<Consultation> reject(@RequestParam Long consultationId,
                                       @RequestParam Long doctorId,
                                       @RequestParam(required = false) String reason) {
        Consultation consultation = crossCampusService.rejectCrossCampusConsultation(consultationId, doctorId, reason);
        return Result.ok(consultation);
    }

    @PostMapping("/consultation/cancel")
    public Result<Consultation> cancel(@RequestParam Long consultationId, @RequestParam Long patientId) {
        Consultation consultation = crossCampusService.cancelCrossCampusConsultation(consultationId, patientId);
        return Result.ok(consultation);
    }

    @GetMapping("/consultation/{id}")
    public Result<ConsultationVO> detail(@PathVariable Long id) {
        ConsultationVO vo = crossCampusService.getCrossCampusDetail(id);
        return Result.ok(vo);
    }

    @GetMapping("/consultation/target-campus/{campusId}")
    public Result<List<ConsultationVO>> targetCampusList(@PathVariable Long campusId,
                                                          @RequestParam(required = false) Integer status) {
        List<ConsultationVO> list = crossCampusService.getTargetCampusConsultations(campusId, status);
        return Result.ok(list);
    }

    @GetMapping("/consultation/source-campus/{campusId}")
    public Result<List<ConsultationVO>> sourceCampusList(@PathVariable Long campusId,
                                                          @RequestParam(required = false) Integer status) {
        List<ConsultationVO> list = crossCampusService.getSourceCampusConsultations(campusId, status);
        return Result.ok(list);
    }

    @GetMapping("/consultation/doctor/{doctorId}")
    public Result<List<ConsultationVO>> doctorList(@PathVariable Long doctorId,
                                                    @RequestParam(required = false) Integer status) {
        List<ConsultationVO> list = crossCampusService.getCrossCampusByDoctorId(doctorId, status);
        return Result.ok(list);
    }

    @GetMapping("/consultation/patient/{patientId}")
    public Result<List<ConsultationVO>> patientList(@PathVariable Long patientId,
                                                     @RequestParam(required = false) Integer status) {
        List<ConsultationVO> list = crossCampusService.getPatientCrossCampusConsultations(patientId, status);
        return Result.ok(list);
    }

    @GetMapping("/schedule/doctor/{doctorId}")
    public Result<DoctorScheduleVO> doctorSchedule(@PathVariable Long doctorId,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DoctorScheduleVO vo = crossCampusService.getDoctorSchedule(doctorId, date);
        return Result.ok(vo);
    }

    @GetMapping("/schedule/campus/{campusId}")
    public Result<List<DoctorScheduleVO>> campusSchedules(@PathVariable Long campusId,
                                                           @RequestParam(required = false) String department,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DoctorScheduleVO> list = crossCampusService.getCampusDoctorSchedules(campusId, department, date);
        return Result.ok(list);
    }

    @GetMapping("/schedule/doctor/{doctorId}/time-slots")
    public Result<List<TimeSlotVO>> doctorTimeSlots(@PathVariable Long doctorId,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<TimeSlotVO> list = crossCampusService.getDoctorAvailableTimeSlots(doctorId, date);
        return Result.ok(list);
    }

    @GetMapping("/hospitals")
    public Result<List<Hospital>> hospitalList() {
        return Result.ok(hospitalRepository.findAll());
    }

    @GetMapping("/campuses")
    public Result<List<Campus>> campusList(@RequestParam(required = false) Long hospitalId) {
        List<Campus> campuses;
        if (hospitalId != null) {
            campuses = campusRepository.findByHospitalId(hospitalId).stream()
                    .filter(c -> c.getStatus() != null && c.getStatus() == 1)
                    .collect(Collectors.toList());
        } else {
            campuses = campusRepository.findAll().stream()
                    .filter(c -> c.getStatus() != null && c.getStatus() == 1)
                    .collect(Collectors.toList());
        }
        return Result.ok(campuses);
    }

    @GetMapping("/campus/{campusId}/other-doctors")
    public Result<List<DoctorVO>> otherCampusDoctors(@PathVariable Long campusId,
                                                      @RequestParam Long hospitalId,
                                                      @RequestParam(required = false) String department) {
        List<Doctor> doctors;
        if (department != null && !department.isEmpty()) {
            doctors = doctorRepository.findOtherCampusDoctors(hospitalId, campusId).stream()
                    .filter(d -> department.equals(d.getDepartment()))
                    .collect(Collectors.toList());
        } else {
            doctors = doctorRepository.findOtherCampusDoctors(hospitalId, campusId);
        }

        List<DoctorVO> voList = new ArrayList<>();
        for (Doctor doctor : doctors) {
            DoctorVO vo = new DoctorVO();
            vo.setId(doctor.getId());
            vo.setUserId(doctor.getUserId());
            vo.setTitle(doctor.getTitle());
            vo.setSpecialty(doctor.getSpecialty());
            vo.setDepartment(doctor.getDepartment());
            vo.setHospitalId(doctor.getHospitalId());
            vo.setCampusId(doctor.getCampusId());
            vo.setStatus(doctor.getStatus());

            User user = userRepository.findById(doctor.getUserId()).orElse(null);
            if (user != null) {
                vo.setName(user.getRealName());
                vo.setAvatarUrl(user.getAvatarUrl());
            }

            Hospital hospital = hospitalRepository.findById(doctor.getHospitalId()).orElse(null);
            if (hospital != null) {
                vo.setHospitalName(hospital.getName());
            }

            Campus campus = campusRepository.findById(doctor.getCampusId()).orElse(null);
            if (campus != null) {
                vo.setCampusName(campus.getName());
            }

            voList.add(vo);
        }
        return Result.ok(voList);
    }

    @GetMapping("/campus/{campusId}/doctors")
    public Result<List<DoctorVO>> campusDoctors(@PathVariable Long campusId,
                                                 @RequestParam(required = false) String department) {
        List<Doctor> doctors;
        if (department != null && !department.isEmpty()) {
            doctors = doctorRepository.findByDepartmentAndCampusIdAndStatus(department, campusId, 1);
        } else {
            doctors = doctorRepository.findByCampusIdAndStatus(campusId, 1);
        }

        List<DoctorVO> voList = new ArrayList<>();
        for (Doctor doctor : doctors) {
            DoctorVO vo = convertToDoctorVO(doctor);
            voList.add(vo);
        }
        return Result.ok(voList);
    }

    private DoctorVO convertToDoctorVO(Doctor doctor) {
        DoctorVO vo = new DoctorVO();
        vo.setId(doctor.getId());
        vo.setUserId(doctor.getUserId());
        vo.setTitle(doctor.getTitle());
        vo.setSpecialty(doctor.getSpecialty());
        vo.setDepartment(doctor.getDepartment());
        vo.setHospitalId(doctor.getHospitalId());
        vo.setCampusId(doctor.getCampusId());
        vo.setStatus(doctor.getStatus());

        User user = userRepository.findById(doctor.getUserId()).orElse(null);
        if (user != null) {
            vo.setName(user.getRealName());
            vo.setAvatarUrl(user.getAvatarUrl());
        }

        Hospital hospital = hospitalRepository.findById(doctor.getHospitalId()).orElse(null);
        if (hospital != null) {
            vo.setHospitalName(hospital.getName());
        }

        Campus campus = campusRepository.findById(doctor.getCampusId()).orElse(null);
        if (campus != null) {
            vo.setCampusName(campus.getName());
        }
        return vo;
    }
}
