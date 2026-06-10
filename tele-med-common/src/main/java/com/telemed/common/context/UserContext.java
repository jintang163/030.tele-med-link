package com.telemed.common.context;

public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<Long> HOSPITAL_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> CAMPUS_ID = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void setUsername(String username) {
        USERNAME.set(username);
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    public static void setRole(String role) {
        ROLE.set(role);
    }

    public static String getRole() {
        return ROLE.get();
    }

    public static void setHospitalId(Long hospitalId) {
        HOSPITAL_ID.set(hospitalId);
    }

    public static Long getHospitalId() {
        return HOSPITAL_ID.get();
    }

    public static void setCampusId(Long campusId) {
        CAMPUS_ID.set(campusId);
    }

    public static Long getCampusId() {
        return CAMPUS_ID.get();
    }

    public static boolean isAdmin() {
        String role = ROLE.get();
        return "ADMIN".equals(role);
    }

    public static boolean isDoctor() {
        String role = ROLE.get();
        return "DOCTOR".equals(role);
    }

    public static boolean isPatient() {
        String role = ROLE.get();
        return "PATIENT".equals(role);
    }

    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
        ROLE.remove();
        HOSPITAL_ID.remove();
        CAMPUS_ID.remove();
    }
}
