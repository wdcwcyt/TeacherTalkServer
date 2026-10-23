package com.zl.teacherttalk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TeacherTalkServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeacherTalkServerApplication.class, args);
    }
}
