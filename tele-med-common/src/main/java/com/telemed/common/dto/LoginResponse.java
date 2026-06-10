package com.telemed.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private Long userId;

    private String username;

    private String realName;

    private String role;

    private Long hospitalId;

    private Long campusId;

    private String hospitalName;

    private String campusName;

    private String department;

    private String title;

    private Long doctorId;
}
