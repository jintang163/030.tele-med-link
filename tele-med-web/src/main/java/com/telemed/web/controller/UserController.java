package com.telemed.web.controller;

import com.telemed.common.result.Result;
import com.telemed.common.util.AesEncryptUtil;
import com.telemed.model.entity.Doctor;
import com.telemed.model.entity.Patient;
import com.telemed.model.entity.User;
import com.telemed.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AesEncryptUtil aesEncryptUtil;

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return Result.ok(user);
    }

    @GetMapping("/username/{username}")
    public Result<User> getByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return Result.ok(user);
    }

    @GetMapping("/doctor/{userId}")
    public Result<Doctor> getDoctor(@PathVariable Long userId) {
        Doctor doctor = userService.getDoctorByUserId(userId);
        return Result.ok(doctor);
    }

    @GetMapping("/patient/{userId}")
    public Result<Patient> getPatient(@PathVariable Long userId) {
        Patient patient = userService.getPatientByUserId(userId);
        return Result.ok(patient);
    }

    @GetMapping("/patient/openid/{openId}")
    public Result<Patient> getPatientByOpenId(@PathVariable String openId) {
        Patient patient = userService.getPatientByOpenId(openId);
        return Result.ok(patient);
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (!password.equals(user.getPassword())) {
            return Result.fail("密码错误");
        }
        return Result.ok(user);
    }
}
