package com.telemed.service;

import com.telemed.model.entity.Doctor;
import com.telemed.model.entity.Patient;
import com.telemed.model.entity.User;

public interface UserService {

    User getUserById(Long id);

    User getUserByUsername(String username);

    Doctor getDoctorByUserId(Long userId);

    Patient getPatientByUserId(Long userId);

    Patient getPatientByOpenId(String openId);
}
