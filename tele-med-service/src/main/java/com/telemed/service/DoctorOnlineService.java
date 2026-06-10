package com.telemed.service;

import java.util.List;
import java.util.Map;

public interface DoctorOnlineService {

    void setOnline(Long doctorId, Long hospitalId, Long campusId);

    void setOffline(Long doctorId);

    boolean isOnline(Long doctorId);

    List<Long> getOnlineDoctorIdsByHospital(Long hospitalId);

    List<Long> getOnlineDoctorIdsByCampus(Long campusId);

    Map<String, Integer> getOnlineCountGroupByCampus(Long hospitalId);

    long getOnlineCount();

    void updateHeartbeat(Long doctorId);
}
