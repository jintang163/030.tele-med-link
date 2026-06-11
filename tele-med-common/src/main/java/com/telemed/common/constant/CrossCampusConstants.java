package com.telemed.common.constant;

public class CrossCampusConstants {

    public static final int DOCTOR_ROLE_PRIMARY = 1;
    public static final int DOCTOR_ROLE_ASSISTANT = 2;

    public static final int JOIN_STATUS_PENDING = 0;
    public static final int JOIN_STATUS_ACCEPTED = 1;
    public static final int JOIN_STATUS_REJECTED = 2;
    public static final int JOIN_STATUS_JOINED = 3;
    public static final int JOIN_STATUS_LEFT = 4;

    public static final String CAMPUS_TAG_SEPARATOR = "_TO_";

    public static final String ROOM_ID_PREFIX = "CROSS-CAMPUS";

    public static final String MQ_TOPIC_CROSS_CAMPUS_NOTIFY = "CROSS_CAMPUS_CONSULTATION_NOTIFY";

    public static final int CONSULTATION_TYPE_CROSS_CAMPUS = 2;

    public static final int DEFAULT_TIMEOUT_MINUTES = 30;
}
