package com.telemed.common.constant;

public class FaceVerifyConstants {

    public static final int VERIFY_TYPE_ID_CARD_FACE = 1;
    public static final int VERIFY_TYPE_LIVE_DETECT = 2;
    public static final int VERIFY_TYPE_LIVE_ID_COMPARE = 3;

    public static final int RESULT_PASS = 1;
    public static final int RESULT_FAIL = 0;

    public static final int LOCK_STATUS_UNLOCKED = 0;
    public static final int LOCK_STATUS_LOCKED = 1;

    public static final int TOKEN_TYPE_PDF_DOWNLOAD = 1;
    public static final int TOKEN_TYPE_CONSULTATION_VIEW = 2;
    public static final int TOKEN_TYPE_GENERAL = 3;

    public static final int TOKEN_UNUSED = 0;
    public static final int TOKEN_USED = 1;

    public static final int MAX_FAILURE_COUNT = 3;

    public static final long DEFAULT_TOKEN_EXPIRE_MINUTES = 5;

    public static final double DEFAULT_SIMILARITY_THRESHOLD = 0.8;

    private FaceVerifyConstants() {
    }
}
