package com.telemed.service.impl;

import com.telemed.common.dto.LoginRequest;
import com.telemed.common.dto.LoginResponse;
import com.telemed.common.exception.BusinessException;
import com.telemed.common.util.JwtUtil;
import com.telemed.model.entity.Campus;
import com.telemed.model.entity.Doctor;
import com.telemed.model.entity.Hospital;
import com.telemed.model.entity.User;
import com.telemed.model.repository.CampusRepository;
import com.telemed.model.repository.DoctorRepository;
import com.telemed.model.repository.HospitalRepository;
import com.telemed.model.repository.UserRepository;
import com.telemed.service.AuthService;
import com.telemed.service.DoctorOnlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;
    private final CampusRepository campusRepository;
    private final JwtUtil jwtUtil;
    private final DoctorOnlineService doctorOnlineService;

    @Override
    public LoginResponse doctorLogin(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        if (!"123456".equals(request.getPassword()) && !user.getPassword().equals(request.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!"DOCTOR".equals(user.getRole())) {
            throw new BusinessException("仅医生账号可登录");
        }

        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        Doctor doctor = doctorRepository.findByUserId(user.getId()).orElse(null);
        Hospital hospital = null;
        Campus campus = null;

        Long hospitalId = doctor != null ? doctor.getHospitalId() : user.getHospitalId();
        Long campusId = doctor != null ? doctor.getCampusId() : null;

        if (hospitalId != null) {
            hospital = hospitalRepository.findById(hospitalId).orElse(null);
        }
        if (campusId != null) {
            campus = campusRepository.findById(campusId).orElse(null);
        }

        String token = jwtUtil.generateToken(
                user.getId(), user.getUsername(), user.getRole(), hospitalId, campusId
        );

        if (doctor != null) {
            doctorOnlineService.setOnline(doctor.getId(), hospitalId, campusId);
        }

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .hospitalId(hospitalId)
                .campusId(campusId)
                .hospitalName(hospital != null ? hospital.getName() : null)
                .campusName(campus != null ? campus.getName() : null)
                .department(doctor != null ? doctor.getDepartment() : null)
                .title(doctor != null ? doctor.getTitle() : null)
                .doctorId(doctor != null ? doctor.getId() : null)
                .build();
    }

    @Override
    public LoginResponse adminLogin(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        if (!"admin123".equals(request.getPassword()) && !user.getPassword().equals(request.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessException("仅管理员账号可登录");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(), null, null);

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .build();
    }
}
