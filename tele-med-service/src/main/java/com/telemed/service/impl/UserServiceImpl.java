package com.telemed.service.impl;

import com.telemed.common.exception.BusinessException;
import com.telemed.model.entity.Doctor;
import com.telemed.model.entity.Patient;
import com.telemed.model.entity.User;
import com.telemed.model.repository.DoctorRepository;
import com.telemed.model.repository.PatientRepository;
import com.telemed.model.repository.UserRepository;
import com.telemed.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    @Override
    public Doctor getDoctorByUserId(Long userId) {
        return doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("医生信息不存在"));
    }

    @Override
    public Patient getPatientByUserId(Long userId) {
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("患者信息不存在"));
    }

    @Override
    public Patient getPatientByOpenId(String openId) {
        return patientRepository.findByOpenId(openId)
                .orElseThrow(() -> new BusinessException("患者信息不存在"));
    }
}
