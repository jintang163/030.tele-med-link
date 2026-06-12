package com.telemed.service.impl;

import cn.hutool.core.codec.Base64;
import com.telemed.common.exception.BusinessException;
import com.telemed.service.Sm2SignatureService;
import jakarta.annotation.PostConstruct;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.custom.gm.SM2P256V1Curve;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;

@Service
public class Sm2SignatureServiceImpl implements Sm2SignatureService {

    private static final SM2P256V1Curve CURVE = new SM2P256V1Curve();
    private static final BigInteger GX = new BigInteger("32C4AE2C1F1981195F9904466A39C9948FE30BBFF2660BE1715A4589334C74C7", 16);
    private static final BigInteger GY = new BigInteger("BC3736A2F4F6779C59BDCEE36B692153D0A9877CC62A474002DF32E52139F0A0", 16);
    private static final BigInteger N = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFF7203DF6B21C6052B53BBF40939D54123", 16);
    private static final ECDomainParameters DOMAIN_PARAMS;

    static {
        Security.addProvider(new BouncyCastleProvider());
        ECPoint G = CURVE.createPoint(GX, GY);
        DOMAIN_PARAMS = new ECDomainParameters(CURVE, G, N);
    }

    @PostConstruct
    public void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
            ECParameterSpec ecSpec = new ECParameterSpec(
                    DOMAIN_PARAMS.getCurve(),
                    DOMAIN_PARAMS.getG(),
                    DOMAIN_PARAMS.getN()
            );
            keyPairGenerator.initialize(ecSpec, new SecureRandom());
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new BusinessException("生成SM2密钥对失败: " + e.getMessage());
        }
    }

    @Override
    public String getPublicKeyHex(KeyPair keyPair) {
        BCECPublicKey publicKey = (BCECPublicKey) keyPair.getPublic();
        ECPoint ecPoint = publicKey.getQ();
        byte[] encoded = ecPoint.getEncoded(false);
        return org.bouncycastle.util.encoders.Hex.toHexString(encoded);
    }

    @Override
    public String getPrivateKeyHex(KeyPair keyPair) {
        BCECPrivateKey privateKey = (BCECPrivateKey) keyPair.getPrivate();
        BigInteger d = privateKey.getD();
        String hex = d.toString(16);
        while (hex.length() < 64) {
            hex = "0" + hex;
        }
        return hex;
    }

    @Override
    public String sign(byte[] data, String privateKeyHex) {
        try {
            BigInteger d = new BigInteger(privateKeyHex, 16);
            ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(d, DOMAIN_PARAMS);

            SM2Signer signer = new SM2Signer();
            signer.init(true, privateKeyParameters);
            signer.update(data, 0, data.length);
            byte[] signature = signer.generateSignature();

            return org.bouncycastle.util.encoders.Hex.toHexString(signature);
        } catch (Exception e) {
            throw new BusinessException("SM2签名失败: " + e.getMessage());
        }
    }

    @Override
    public String sign(String data, String privateKeyHex) {
        return sign(data.getBytes(java.nio.charset.StandardCharsets.UTF_8), privateKeyHex);
    }

    @Override
    public boolean verify(byte[] data, String publicKeyHex, String signatureHex) {
        try {
            byte[] publicKeyBytes = org.bouncycastle.util.encoders.Hex.decode(publicKeyHex);
            ECPoint ecPoint = CURVE.decodePoint(publicKeyBytes);
            ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(ecPoint, DOMAIN_PARAMS);

            byte[] signatureBytes = org.bouncycastle.util.encoders.Hex.decode(signatureHex);

            SM2Signer signer = new SM2Signer();
            signer.init(false, publicKeyParameters);
            signer.update(data, 0, data.length);
            return signer.verifySignature(signatureBytes);
        } catch (Exception e) {
            throw new BusinessException("SM2验签失败: " + e.getMessage());
        }
    }

    @Override
    public boolean verify(String data, String publicKeyHex, String signatureHex) {
        return verify(data.getBytes(java.nio.charset.StandardCharsets.UTF_8), publicKeyHex, signatureHex);
    }

    @Override
    public byte[] sm3Hash(byte[] data) {
        SM3Digest digest = new SM3Digest();
        digest.update(data, 0, data.length);
        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        return result;
    }

    @Override
    public String sm3HashHex(String data) {
        byte[] hash = sm3Hash(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return org.bouncycastle.util.encoders.Hex.toHexString(hash);
    }
}
