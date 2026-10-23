package com.zl.teacherttalk.service;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.zl.teacherttalk.config.AliyunSmsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AliyunSmsService {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsService.class);
    private static final String COUNTRY_CODE = "86";

    private final AliyunSmsProperties properties;
    private final Client client;

    public AliyunSmsService(AliyunSmsProperties properties) {
        this.properties = properties;
        requireConfigured(properties.accessKeyId(), "ALIYUN_ACCESS_KEY_ID");
        requireConfigured(properties.accessKeySecret(), "ALIYUN_ACCESS_KEY_SECRET");
        requireConfigured(properties.signName(), "ALIYUN_SMS_SIGN_NAME");
        requireConfigured(properties.templateCode(), "ALIYUN_SMS_TEMPLATE_CODE");
        requireConfigured(properties.templateParam(), "ALIYUN_SMS_TEMPLATE_PARAM");

        Config config = new Config()
                .setAccessKeyId(properties.accessKeyId())
                .setAccessKeySecret(properties.accessKeySecret())
                .setEndpoint(properties.endpoint())
                .setRegionId(properties.regionId());
        try {
            this.client = new Client(config);
        } catch (Exception exception) {
            throw new IllegalStateException("阿里云短信客户端初始化失败", exception);
        }
    }

    public void sendCode(String phone) {
        SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                .setCountryCode(COUNTRY_CODE)
                .setPhoneNumber(phone)
                .setSignName(properties.signName())
                .setTemplateCode(properties.templateCode())
                .setTemplateParam(properties.templateParam())
                .setCodeLength(properties.codeLength())
                .setCodeType(properties.codeType())
                .setValidTime(properties.validTimeSeconds())
                .setDuplicatePolicy(properties.duplicatePolicy())
                .setInterval(properties.resendIntervalSeconds())
                .setReturnVerifyCode(false);

        if (properties.schemeName() != null && !properties.schemeName().isBlank()) {
            request.setSchemeName(properties.schemeName());
        }

        try {
            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCode(request);
            requireProviderSuccess(response);
            String requestId = response.getBody().getModel() == null
                    ? null
                    : response.getBody().getModel().getRequestId();
            log.info("短信验证码发送请求成功: phone={}, requestId={}", maskPhone(phone), requestId);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("短信服务暂时不可用", exception);
        }
    }

    public boolean verifyCode(String phone, String code) {
        CheckSmsVerifyCodeRequest request = new CheckSmsVerifyCodeRequest()
                .setCountryCode(COUNTRY_CODE)
                .setPhoneNumber(phone)
                .setVerifyCode(code);

        if (properties.schemeName() != null && !properties.schemeName().isBlank()) {
            request.setSchemeName(properties.schemeName());
        }

        try {
            CheckSmsVerifyCodeResponse response = client.checkSmsVerifyCode(request);
            requireProviderSuccess(response);
            return response.getBody().getModel() != null
                    && "PASS".equals(response.getBody().getModel().getVerifyResult());
        } catch (TeaException exception) {
            return handleVerifyException(phone, exception);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("短信服务暂时不可用", exception);
        }
    }

    private void requireProviderSuccess(SendSmsVerifyCodeResponse response) {
        if (response == null
                || response.getBody() == null
                || !Boolean.TRUE.equals(response.getBody().getSuccess())
                || !"OK".equals(response.getBody().getCode())) {
            throw new IllegalStateException("阿里云短信发送接口返回失败");
        }
    }

    private void requireProviderSuccess(CheckSmsVerifyCodeResponse response) {
        if (response == null
                || response.getBody() == null
                || !Boolean.TRUE.equals(response.getBody().getSuccess())
                || !"OK".equals(response.getBody().getCode())) {
            throw new IllegalStateException("阿里云验证码核验接口返回失败");
        }
    }

    private static void requireConfigured(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(variableName + " 未配置");
        }
    }

    private boolean handleVerifyException(String phone, TeaException exception) {
        if ("400".equals(exception.getCode())
                && exception.getMessage() != null
                && exception.getMessage().contains("验证失败")) {
            log.warn("验证码核验未通过: phone={}", maskPhone(phone));
            return false;
        }
        throw new IllegalStateException("短信服务暂时不可用", exception);
    }

    private static String maskPhone(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
