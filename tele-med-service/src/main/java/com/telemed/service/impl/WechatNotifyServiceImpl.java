package com.telemed.service.impl;

import com.telemed.model.entity.Doctor;
import com.telemed.model.entity.Patient;
import com.telemed.model.entity.User;
import com.telemed.model.repository.DoctorRepository;
import com.telemed.model.repository.PatientRepository;
import com.telemed.model.repository.UserRepository;
import com.telemed.service.WechatNotifyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;

@Service
public class WechatNotifyServiceImpl implements WechatNotifyService {

    @Value("${wechat.appId}")
    private String appId;

    @Value("${wechat.appSecret}")
    private String appSecret;

    @Value("${wechat.subscribeMessageTemplateId}")
    private String templateId;

    private volatile String cachedAccessToken;
    private volatile long tokenExpireTime;

    private final RestTemplate restTemplate = new RestTemplate();
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public WechatNotifyServiceImpl(DoctorRepository doctorRepository,
                                   PatientRepository patientRepository,
                                   UserRepository userRepository) {
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String getAccessToken() {
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedAccessToken;
        }
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appId + "&secret=" + appSecret;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> body = response.getBody();
        cachedAccessToken = (String) body.get("access_token");
        tokenExpireTime = System.currentTimeMillis() + 7000 * 1000L;
        return cachedAccessToken;
    }

    @Override
    public void notifyDoctorNewAppointment(Long doctorId, String patientName, LocalDate date, String timeSlot) {
        Doctor doctor = doctorRepository.findById(doctorId).orElseThrow();
        User user = userRepository.findById(doctor.getUserId()).orElseThrow();
        String openId = user.getOpenId();
        sendSubscribeMessage(openId, patientName, date.toString(), timeSlot);
    }

    @Override
    public void remindPatientConsultation(Long patientId, String doctorName, LocalDate date, String timeSlot) {
        Patient patient = patientRepository.findById(patientId).orElseThrow();
        String openId = patient.getOpenId();
        sendSubscribeMessage(openId, doctorName, date.toString(), timeSlot);
    }

    @Override
    public void notifyDoctorNewConsultation(Long doctorId, String patientName, String sourceCampusName, String date, String timeSlot) {
        try {
            Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
            if (doctor == null) {
                return;
            }
            User user = userRepository.findById(doctor.getUserId()).orElse(null);
            if (user == null || user.getOpenId() == null) {
                return;
            }
            String openId = user.getOpenId();
            String notifyContent = String.format("跨院区会诊邀请-%s", sourceCampusName);
            sendSubscribeMessage(openId, notifyContent, date, timeSlot);
        } catch (Exception e) {
        }
    }

    @Override
    public void notifyCrossCampusConsultationResult(Long patientId, String doctorName, boolean accepted, String reason) {
        try {
            Patient patient = patientRepository.findById(patientId).orElse(null);
            if (patient == null || patient.getOpenId() == null) {
                return;
            }
            String openId = patient.getOpenId();
            String result = accepted ? "会诊已确认" : "会诊被拒绝-" + (reason != null ? reason : "");
            sendSubscribeMessage(openId, result, doctorName, accepted ? "请准时参加" : "请另行预约");
        } catch (Exception e) {
        }
    }

    private void sendSubscribeMessage(String openId, String thing1Value, String date2Value, String thing3Value) {
        String accessToken = getAccessToken();
        String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken;
        Map<String, Object> body = Map.of(
                "touser", openId,
                "template_id", templateId,
                "data", Map.of(
                        "thing1", Map.of("value", thing1Value),
                        "date2", Map.of("value", date2Value),
                        "thing3", Map.of("value", thing3Value)
                )
        );
        restTemplate.postForEntity(url, body, Map.class);
    }
}
