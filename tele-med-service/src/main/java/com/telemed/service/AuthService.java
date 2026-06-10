package com.telemed.service;

import com.telemed.common.dto.LoginRequest;
import com.telemed.common.dto.LoginResponse;

public interface AuthService {

    LoginResponse doctorLogin(LoginRequest request);

    LoginResponse adminLogin(LoginRequest request);
}
