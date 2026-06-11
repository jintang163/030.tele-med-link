package com.telemed.common.vo.mediasoup;

import lombok.Data;

import java.util.List;

@Data
public class TurnServerVO {

    private List<String> urls;

    private String username;

    private String credential;

    private String credentialType = "password";
}
