package com.telemed.common.util;

import cn.hutool.core.util.StrUtil;

import java.awt.image.BufferedImage;

public class QrCodeUtil {

    public static BufferedImage generateQrCode(String content, int width, int height) {
        if (StrUtil.isBlank(content)) {
            throw new IllegalArgumentException("二维码内容不能为空");
        }
        return cn.hutool.extra.qrcode.QrCodeUtil.generate(content, width, height);
    }

    public static byte[] generateQrCodeBytes(String content, int width, int height, String imageType) {
        if (StrUtil.isBlank(content)) {
            throw new IllegalArgumentException("二维码内容不能为空");
        }
        return cn.hutool.extra.qrcode.QrCodeUtil.generatePng(content, width, height);
    }

    public static byte[] generateQrCodePng(String content, int width, int height) {
        return generateQrCodeBytes(content, width, height, "png");
    }

    public static String decodeQrCode(BufferedImage image) {
        return cn.hutool.extra.qrcode.QrCodeUtil.decode(image);
    }
}
