package com.zl.teacherttalk.service;

import com.zl.teacherttalk.common.BusinessException;
import com.zl.teacherttalk.common.ErrorCode;
import com.zl.teacherttalk.config.AliyunSmsProperties;
import com.zl.teacherttalk.dto.AuthResponse;
import com.zl.teacherttalk.dto.LoginRequest;
import com.zl.teacherttalk.dto.RegisterRequest;
import com.zl.teacherttalk.dto.SendSmsCodeResponse;
import com.zl.teacherttalk.dto.UserResponse;
import com.zl.teacherttalk.entity.User;
import com.zl.teacherttalk.mapper.UserMapper;
import com.zl.teacherttalk.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Set<String> ALLOWED_ROLES = Set.of("STUDENT", "PARENT");
    private static final String DEFAULT_NICKNAME_PREFIX = "新用户";
    private static final String NICKNAME_RANDOM_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int NICKNAME_RANDOM_LENGTH = 6;

    private final UserMapper userMapper;
    private final AliyunSmsService smsService;
    private final JwtService jwtService;
    private final long validTimeSeconds;
    private final long resendIntervalSeconds;

    public AuthService(
            UserMapper userMapper,
            AliyunSmsService smsService,
            JwtService jwtService,
            AliyunSmsProperties smsProperties
    ) {
        this.userMapper = userMapper;
        this.smsService = smsService;
        this.jwtService = jwtService;
        this.validTimeSeconds = smsProperties.validTimeSeconds();
        this.resendIntervalSeconds = smsProperties.resendIntervalSeconds();
    }

    public SendSmsCodeResponse sendSmsCode(String phone) {
        smsService.sendCode(phone);
        return new SendSmsCodeResponse(validTimeSeconds, resendIntervalSeconds);
    }

    public AuthResponse register(RegisterRequest request) {
        String role = validateRole(request.role());
        String nickname = resolveNickname(request.nickname());

        if (!smsService.verifyCode(request.phone(), request.code())) {
            log.warn("注册验证码核验失败: phone={}", maskPhone(request.phone()));
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "验证码错误或已过期");
        }
        if (userMapper.findByPhone(request.phone()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "该手机号已注册");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setPhone(request.phone());
        user.setNickname(nickname);
        user.setRole(role);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        try {
            if (userMapper.insert(user) != 1 || user.getId() == null) {
                throw new IllegalStateException("用户写入失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "该手机号已注册");
        }

        log.info("用户注册成功: userId={}", user.getId());
        return issueAuth(user);
    }

    public AuthResponse login(LoginRequest request) {
        if (!smsService.verifyCode(request.phone(), request.code())) {
            log.warn("登录验证码核验失败: phone={}", maskPhone(request.phone()));
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "验证码错误或已过期");
        }

        User user = userMapper.findByPhone(request.phone());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "手机号未注册，请先注册");
        }
        log.info("用户登录成功: userId={}", user.getId());
        return issueAuth(user);
    }

    public UserResponse getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return UserResponse.from(user);
    }

    private AuthResponse issueAuth(User user) {
        String token = jwtService.createToken(user.getId(), user.getRole());
        return new AuthResponse(token, UserResponse.from(user));
    }

    private String validateRole(String role) {
        if (!ALLOWED_ROLES.contains(role)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "role 只允许 STUDENT 或 PARENT");
        }
        return role;
    }

    private String resolveNickname(String nickname) {
        String normalizedNickname = nickname == null ? "" : nickname.strip();
        if (normalizedNickname.isEmpty()) {
            return generateDefaultNickname();
        }
        if (normalizedNickname.length() < 2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "nickname 至少需要 2 个字符");
        }
        return normalizedNickname;
    }

    private String generateDefaultNickname() {
        StringBuilder nickname = new StringBuilder(DEFAULT_NICKNAME_PREFIX);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int index = 0; index < NICKNAME_RANDOM_LENGTH; index++) {
            int charIndex = random.nextInt(NICKNAME_RANDOM_CHARS.length());
            nickname.append(NICKNAME_RANDOM_CHARS.charAt(charIndex));
        }
        return nickname.toString();
    }

    private static String maskPhone(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

}
