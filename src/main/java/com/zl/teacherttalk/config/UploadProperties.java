package com.zl.teacherttalk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(
        Path directory,
        DataSize maxImageSize,
        int thumbnailSize,
        int maxSourceDimension
) {

    public UploadProperties {
        if (directory == null) {
            throw new IllegalArgumentException("上传目录不能为空");
        }
        directory = directory.toAbsolutePath().normalize();

        if (maxImageSize == null || maxImageSize.toBytes() <= 0) {
            throw new IllegalArgumentException("图片大小限制必须大于 0");
        }
        if (thumbnailSize <= 0) {
            throw new IllegalArgumentException("缩略图尺寸必须大于 0");
        }
        if (maxSourceDimension <= 0) {
            throw new IllegalArgumentException("图片最大边长必须大于 0");
        }
    }
}
