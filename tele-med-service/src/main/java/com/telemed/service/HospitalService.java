package com.telemed.service;

import com.telemed.model.entity.Campus;
import com.telemed.model.entity.Hospital;

import java.util.List;

public interface HospitalService {

    List<Hospital> getAllHospitals();

    Hospital getHospitalById(Long id);

    List<Campus> getCampusesByHospitalId(Long hospitalId);

    Campus getCampusById(Long id);
}
