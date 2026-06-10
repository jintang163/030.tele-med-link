package com.telemed.service.impl;

import com.telemed.common.exception.BusinessException;
import com.telemed.model.entity.Campus;
import com.telemed.model.entity.Hospital;
import com.telemed.model.repository.CampusRepository;
import com.telemed.model.repository.HospitalRepository;
import com.telemed.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HospitalServiceImpl implements HospitalService {

    private final HospitalRepository hospitalRepository;
    private final CampusRepository campusRepository;

    @Override
    public List<Hospital> getAllHospitals() {
        return hospitalRepository.findByStatus(1);
    }

    @Override
    public Hospital getHospitalById(Long id) {
        return hospitalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("医院不存在"));
    }

    @Override
    public Hospital createHospital(Hospital hospital) {
        hospital.setStatus(1);
        return hospitalRepository.save(hospital);
    }

    @Override
    public Hospital updateHospital(Hospital hospital) {
        Hospital existing = hospitalRepository.findById(hospital.getId())
                .orElseThrow(() -> new BusinessException("医院不存在"));
        if (hospital.getName() != null) {
            existing.setName(hospital.getName());
        }
        if (hospital.getAddress() != null) {
            existing.setAddress(hospital.getAddress());
        }
        if (hospital.getPhone() != null) {
            existing.setPhone(hospital.getPhone());
        }
        if (hospital.getLevel() != null) {
            existing.setLevel(hospital.getLevel());
        }
        if (hospital.getStatus() != null) {
            existing.setStatus(hospital.getStatus());
        }
        return hospitalRepository.save(existing);
    }

    @Override
    public void deleteHospital(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("医院不存在"));
        hospital.setStatus(0);
        hospitalRepository.save(hospital);
    }

    @Override
    public List<Campus> getCampusesByHospitalId(Long hospitalId) {
        return campusRepository.findByHospitalIdAndStatus(hospitalId, 1);
    }

    @Override
    public List<Campus> getCampusesByHospital(Long hospitalId) {
        return campusRepository.findByHospitalIdAndStatus(hospitalId, 1);
    }

    @Override
    public Campus getCampusById(Long id) {
        return campusRepository.findById(id)
                .orElseThrow(() -> new BusinessException("院区不存在"));
    }

    @Override
    public Campus createCampus(Campus campus) {
        campus.setStatus(1);
        return campusRepository.save(campus);
    }

    @Override
    public Campus updateCampus(Campus campus) {
        Campus existing = campusRepository.findById(campus.getId())
                .orElseThrow(() -> new BusinessException("院区不存在"));
        if (campus.getName() != null) {
            existing.setName(campus.getName());
        }
        if (campus.getAddress() != null) {
            existing.setAddress(campus.getAddress());
        }
        if (campus.getPhone() != null) {
            existing.setPhone(campus.getPhone());
        }
        if (campus.getStatus() != null) {
            existing.setStatus(campus.getStatus());
        }
        return campusRepository.save(existing);
    }

    @Override
    public void deleteCampus(Long id) {
        Campus campus = campusRepository.findById(id)
                .orElseThrow(() -> new BusinessException("院区不存在"));
        campus.setStatus(0);
        campusRepository.save(campus);
    }
}
