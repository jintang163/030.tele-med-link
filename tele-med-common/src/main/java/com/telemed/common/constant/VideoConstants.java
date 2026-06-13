package com.telemed.common.constant;

public class VideoConstants {

    public static final int DEFAULT_SEGMENT_DURATION = 300;

    public static final int DEFAULT_RETENTION_DAYS = 90;

    public static final String SEGMENT_BUCKET = "tele-med-video-segments";

    public static final String HLS_BUCKET = "tele-med-video-hls";

    public static final String WATERMARK_TEXT = "录制中 - 远程会诊 - %s";

    public static final String WEB_MIME_TYPE = "video/webm;codecs=vp9,opus";

    public static final String HLS_MIME_TYPE = "application/vnd.apple.mpegurl";

    public static final String TS_MIME_TYPE = "video/mp2t";

    public static final String AES_KEY_HEADER = "X-Video-Key";

    public static final String AES_IV_HEADER = "X-Video-Iv";

    public static final String AUTH_TOKEN_HEADER = "X-Playback-Token";
}
