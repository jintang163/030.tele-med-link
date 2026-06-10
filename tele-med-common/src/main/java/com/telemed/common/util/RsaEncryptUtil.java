package com.telemed.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class RsaEncryptUtil {

    private static final String ALGORITHM = "RSA";
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    @Value("${encryption.rsaPublicKey}")
    private String rsaPublicKey;

    @Value("${encryption.rsaPrivateKey}")
    private String rsaPrivateKey;

    public String encrypt(String plainText) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(rsaPublicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            PublicKey publicKey = KeyFactory.getInstance(ALGORITHM).generatePublic(keySpec);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("RSA encryption failed", e);
        }
    }

    public String decrypt(String cipherText) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(rsaPrivateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            PrivateKey privateKey = KeyFactory.getInstance(ALGORITHM).generatePrivate(keySpec);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("RSA decryption failed", e);
        }
    }
}
