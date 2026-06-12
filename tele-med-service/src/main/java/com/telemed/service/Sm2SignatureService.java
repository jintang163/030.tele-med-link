package com.telemed.service;

import java.security.KeyPair;

public interface Sm2SignatureService {

    KeyPair generateKeyPair();

    String getPublicKeyHex(KeyPair keyPair);

    String getPrivateKeyHex(KeyPair keyPair);

    String sign(byte[] data, String privateKeyHex);

    String sign(String data, String privateKeyHex);

    boolean verify(byte[] data, String publicKeyHex, String signatureHex);

    boolean verify(String data, String publicKeyHex, String signatureHex);

    byte[] sm3Hash(byte[] data);

    String sm3HashHex(String data);
}
