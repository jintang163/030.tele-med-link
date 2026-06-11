package com.telemed.common.constant;

public enum VideoResolution {

    P_720("1280x720", 1280, 720, 2_500_000L, 30),
    P_540("960x540", 960, 540, 1_500_000L, 25),
    P_360("640x360", 640, 360, 800_000L, 20);

    private final String label;
    private final int width;
    private final int height;
    private final long bitrate;
    private final int fps;

    VideoResolution(String label, int width, int height, long bitrate, int fps) {
        this.label = label;
        this.width = width;
        this.height = height;
        this.bitrate = bitrate;
        this.fps = fps;
    }

    public String getLabel() {
        return label;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getBitrate() {
        return bitrate;
    }

    public int getFps() {
        return fps;
    }

    public static VideoResolution downgrade(VideoResolution current) {
        if (current == P_720) {
            return P_540;
        } else if (current == P_540) {
            return P_360;
        }
        return P_360;
    }

    public static VideoResolution upgrade(VideoResolution current) {
        if (current == P_360) {
            return P_540;
        } else if (current == P_540) {
            return P_720;
        }
        return P_720;
    }
}
