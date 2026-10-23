package com.zl.teacherttalk.controller;

import com.zl.teacherttalk.common.Result;
import com.zl.teacherttalk.dto.AuthResponse;
import com.zl.teacherttalk.dto.LoginRequest;
import com.zl.teacherttalk.dto.RegisterRequest;
import com.zl.teacherttalk.dto.SendSmsCodeRequest;
import com.zl.teacherttalk.dto.SendSmsCodeResponse;
import com.zl.teacherttalk.dto.UserResponse;
import com.zl.teacherttalk.security.CurrentUserContext;
import com.zl.teacherttalk.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sms-code")
    public Result<SendSmsCodeResponse> sendSmsCode(
            @Valid @RequestBody SendSmsCodeRequest request
    ) {
        return Result.success(authService.sendSmsCode(request.phone()));
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<UserResponse> currentUser() {
        return Result.success(authService.getCurrentUser(CurrentUserContext.requireUserId()));
    }
}
