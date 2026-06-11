package com.telemed.common.constant;

public class MediasoupConstants {

    public static final int NODE_STATUS_OFFLINE = 0;
    public static final int NODE_STATUS_ONLINE = 1;
    public static final int NODE_STATUS_DRAINING = 2;

    public static final long HEARTBEAT_TIMEOUT_SECONDS = 60;

    public static final int ROUTER_MAX_CONSUMERS = 500;

    public static final int VIDEO_720P_WIDTH = 1280;
    public static final int VIDEO_720P_HEIGHT = 720;
    public static final long VIDEO_720P_BITRATE = 2_500_000L;
    public static final int VIDEO_720P_FPS = 30;

    public static final int VIDEO_360P_WIDTH = 640;
    public static final int VIDEO_360P_HEIGHT = 360;
    public static final long VIDEO_360P_BITRATE = 800_000L;
    public static final int VIDEO_360P_FPS = 20;

    public static final double LOSS_RATE_HIGH_THRESHOLD = 0.05;
    public static final double LOSS_RATE_LOW_THRESHOLD = 0.01;

    public static final long QUALITY_MONITOR_INTERVAL_MS = 3000L;
    public static final long QUALITY_STABLE_DURATION_MS = 10_000L;

    public static final int TURN_DEFAULT_PORT = 3478;

    public static final String WS_PROTOCOL = "ws://";
    public static final String WSS_PROTOCOL = "wss://";
    public static final String HTTP_PROTOCOL = "http://";
    public static final String HTTPS_PROTOCOL = "https://";
}
