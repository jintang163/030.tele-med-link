package com.telemed.common.converter;

import com.telemed.common.util.AesEncryptUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Converter(autoApply = false)
public class EncryptConverter implements AttributeConverter<String, String> {

    private static AesEncryptUtil aesEncryptUtil;

    @Autowired
    public void setAesEncryptUtil(AesEncryptUtil aesEncryptUtil) {
        EncryptConverter.aesEncryptUtil = aesEncryptUtil;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        try {
            return aesEncryptUtil.encrypt(attribute);
        } catch (Exception e) {
            return attribute;
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        try {
            return aesEncryptUtil.decrypt(dbData);
        } catch (Exception e) {
            return dbData;
        }
    }
}
