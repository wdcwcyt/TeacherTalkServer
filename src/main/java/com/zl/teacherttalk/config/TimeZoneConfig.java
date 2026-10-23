package com.zl.teacherttalk.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {

    private static final String APPLICATION_TIME_ZONE = "Asia/Shanghai";

    @PostConstruct
    public void configureDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(APPLICATION_TIME_ZONE));
    }
}
