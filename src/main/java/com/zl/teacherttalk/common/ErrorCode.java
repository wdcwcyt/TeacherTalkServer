package com.zl.teacherttalk.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    SUCCESS(0, "ok", HttpStatus.OK),
    BAD_REQUEST(400, "参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, "登录已过期，请重新登录", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "无权限执行该操作", HttpStatus.FORBIDDEN),
    NOT_FOUND(404, "请求的资源不存在", HttpStatus.NOT_FOUND),
    CONFLICT(409, "当前状态不允许执行该操作", HttpStatus.CONFLICT),
    INTERNAL_ERROR(500, "服务暂时不可用，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
