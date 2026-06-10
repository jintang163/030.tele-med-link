package com.telemed.web.controller;

import com.telemed.common.dto.LoginRequest;
import com.telemed.common.dto.LoginResponse;
import com.telemed.common.result.Result;
import com.telemed.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/doctor-login")
    public Result<LoginResponse> doctorLogin(@RequestBody LoginRequest request) {
        LoginResponse response = authService.doctorLogin(request);
        return Result.ok(response);
    }

    @PostMapping("/admin-login")
    public Result<LoginResponse> adminLogin(@RequestBody LoginRequest request) {
        LoginResponse response = authService.adminLogin(request);
        return Result.ok(response);
    }
}
