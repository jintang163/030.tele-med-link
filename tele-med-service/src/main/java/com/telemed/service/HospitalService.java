package com.telemed.service;

import com.telemed.model.entity.Campus;
import com.telemed.model.entity.Hospital;

import java.util.List;

public interface HospitalService {

    List<Hospital> getAllHospitals();

    Hospital getHospitalById(Long id);

    Hospital createHospital(Hospital hospital);

    Hospital updateHospital(Hospital hospital);

    void deleteHospital(Long id);

    List<Campus> getCampusesByHospitalId(Long hospitalId);

    List<Campus> getCampusesByHospital(Long hospitalId);

    Campus getCampusById(Long id);

    Campus createCampus(Campus campus);

    Campus updateCampus(Campus campus);

    void deleteCampus(Long id);
}
