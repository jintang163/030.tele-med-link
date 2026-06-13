package com.telemed.common.util;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;

import java.io.File;
import java.io.InputStream;

public class HashUtil {

    private static final Digester SHA256_DIGESTER = new Digester(DigestAlgorithm.SHA256);

    private static final Digester SM3_DIGESTER = new Digester(DigestAlgorithm.SM3);

    public static String sha256Hex(byte[] data) {
        return SHA256_DIGESTER.digestHex(data);
    }

    public static String sha256Hex(InputStream inputStream) {
        return SHA256_DIGESTER.digestHex(inputStream);
    }

    public static String sha256Hex(File file) {
        return SHA256_DIGESTER.digestHex(file);
    }

    public static String sha256Hex(String data) {
        return SHA256_DIGESTER.digestHex(data);
    }

    public static String sm3Hex(byte[] data) {
        return SM3_DIGESTER.digestHex(data);
    }

    public static String sm3Hex(InputStream inputStream) {
        return SM3_DIGESTER.digestHex(inputStream);
    }

    public static String sm3Hex(File file) {
        return SM3_DIGESTER.digestHex(file);
    }

    public static String sm3Hex(String data) {
        return SM3_DIGESTER.digestHex(data);
    }

    public static String md5Hex(byte[] data) {
        return SecureUtil.md5().digestHex(data);
    }

    public static String calculateHash(byte[] data, String algorithm) {
        if ("SM3".equalsIgnoreCase(algorithm)) {
            return sm3Hex(data);
        } else if ("MD5".equalsIgnoreCase(algorithm)) {
            return md5Hex(data);
        } else {
            return sha256Hex(data);
        }
    }
}
