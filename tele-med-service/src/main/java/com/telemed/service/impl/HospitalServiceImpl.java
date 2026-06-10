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
    public List<Campus> getCampusesByHospitalId(Long hospitalId) {
        return campusRepository.findByHospitalIdAndStatus(hospitalId, 1);
    }

    @Override
    public Campus getCampusById(Long id) {
        return campusRepository.findById(id)
                .orElseThrow(() -> new BusinessException("院区不存在"));
    }
}
