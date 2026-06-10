package com.telemed.service.impl;

import com.telemed.service.DoctorOnlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorOnlineServiceImpl implements DoctorOnlineService {

    private static final String DOCTOR_ONLINE_KEY = "telemed:doctor:online:";
    private static final String HOSPITAL_ONLINE_KEY = "telemed:hospital:online:";
    private static final String CAMPUS_ONLINE_KEY = "telemed:campus:online:";
    private static final long ONLINE_TIMEOUT = 60;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void setOnline(Long doctorId, Long hospitalId, Long campusId) {
        String doctorKey = DOCTOR_ONLINE_KEY + doctorId;
        Map<String, Object> info = new HashMap<>();
        info.put("doctorId", doctorId);
        info.put("hospitalId", hospitalId);
        info.put("campusId", campusId);
        info.put("onlineTime", System.currentTimeMillis());
        info.put("lastHeartbeat", System.currentTimeMillis());
        redisTemplate.opsForValue().set(doctorKey, info, ONLINE_TIMEOUT, TimeUnit.MINUTES);

        if (hospitalId != null) {
            String hospitalKey = HOSPITAL_ONLINE_KEY + hospitalId;
            redisTemplate.opsForSet().add(hospitalKey, doctorId.toString());
            redisTemplate.expire(hospitalKey, ONLINE_TIMEOUT, TimeUnit.MINUTES);
        }

        if (campusId != null) {
            String campusKey = CAMPUS_ONLINE_KEY + campusId;
            redisTemplate.opsForSet().add(campusKey, doctorId.toString());
            redisTemplate.expire(campusKey, ONLINE_TIMEOUT, TimeUnit.MINUTES);
        }
    }

    @Override
    public void setOffline(Long doctorId) {
        String doctorKey = DOCTOR_ONLINE_KEY + doctorId;
        Map<String, Object> info = (Map<String, Object>) redisTemplate.opsForValue().get(doctorKey);
        redisTemplate.delete(doctorKey);

        if (info != null) {
            Object hospitalId = info.get("hospitalId");
            if (hospitalId != null) {
                String hospitalKey = HOSPITAL_ONLINE_KEY + hospitalId;
                redisTemplate.opsForSet().remove(hospitalKey, doctorId.toString());
            }
            Object campusId = info.get("campusId");
            if (campusId != null) {
                String campusKey = CAMPUS_ONLINE_KEY + campusId;
                redisTemplate.opsForSet().remove(campusKey, doctorId.toString());
            }
        }
    }

    @Override
    public boolean isOnline(Long doctorId) {
        String doctorKey = DOCTOR_ONLINE_KEY + doctorId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(doctorKey));
    }

    @Override
    public List<Long> getOnlineDoctorIdsByHospital(Long hospitalId) {
        String hospitalKey = HOSPITAL_ONLINE_KEY + hospitalId;
        Set<Object> members = redisTemplate.opsForSet().members(hospitalKey);
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }
        return members.stream()
                .map(Object::toString)
                .filter(id -> isOnline(Long.valueOf(id)))
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getOnlineDoctorIdsByCampus(Long campusId) {
        String campusKey = CAMPUS_ONLINE_KEY + campusId;
        Set<Object> members = redisTemplate.opsForSet().members(campusKey);
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }
        return members.stream()
                .map(Object::toString)
                .filter(id -> isOnline(Long.valueOf(id)))
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Integer> getOnlineCountGroupByCampus(Long hospitalId) {
        return Collections.emptyMap();
    }

    @Override
    public long getOnlineCount() {
        Set<String> keys = Objects.requireNonNull(redisTemplate.keys(DOCTOR_ONLINE_KEY + "*"));
        return keys.size();
    }

    @Override
    public void updateHeartbeat(Long doctorId) {
        String doctorKey = DOCTOR_ONLINE_KEY + doctorId;
        Map<String, Object> info = (Map<String, Object>) redisTemplate.opsForValue().get(doctorKey);
        if (info != null) {
            info.put("lastHeartbeat", System.currentTimeMillis());
            redisTemplate.opsForValue().set(doctorKey, info, ONLINE_TIMEOUT, TimeUnit.MINUTES);

            Object hospitalId = info.get("hospitalId");
            if (hospitalId != null) {
                String hospitalKey = HOSPITAL_ONLINE_KEY + hospitalId;
                redisTemplate.expire(hospitalKey, ONLINE_TIMEOUT, TimeUnit.MINUTES);
            }
            Object campusId = info.get("campusId");
            if (campusId != null) {
                String campusKey = CAMPUS_ONLINE_KEY + campusId;
                redisTemplate.expire(campusKey, ONLINE_TIMEOUT, TimeUnit.MINUTES);
            }
        }
    }
}
