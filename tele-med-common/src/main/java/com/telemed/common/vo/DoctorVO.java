package com.telemed.common.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DoctorVO {

    private Long id;

    private Long userId;

    private String name;

    private String title;

    private String specialty;

    private String department;

    private Long hospitalId;

    private String hospitalName;

    private Long campusId;

    private String campusName;

    private String avatarUrl;

    private Integer status;

    private LocalDateTime createTime;
}
