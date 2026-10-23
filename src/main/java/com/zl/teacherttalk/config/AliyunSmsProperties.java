package com.zl.teacherttalk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.aliyun.sms")
public record AliyunSmsProperties(
        String accessKeyId,
        String accessKeySecret,
        String endpoint,
        String regionId,
        String signName,
        String templateCode,
        String templateParam,
        String schemeName,
        long codeLength,
        long validTimeSeconds,
        long resendIntervalSeconds,
        long duplicatePolicy,
        long codeType
) {
}
