package com.zl.teacherttalk.service;

import com.zl.teacherttalk.common.BusinessException;
import com.zl.teacherttalk.common.ErrorCode;
import com.zl.teacherttalk.config.UploadProperties;
import com.zl.teacherttalk.dto.FileUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final int FILE_HEADER_LENGTH = 12;
    private static final Pattern ORIGINAL_IMAGE_URL_PATTERN =
            Pattern.compile("^/uploads/[0-9a-f]{32}\\.(?:jpg|png)$");

    private final Path uploadDirectory;
    private final long maxImageBytes;
    private final int thumbnailSize;
    private final int maxSourceDimension;

    public FileStorageService(UploadProperties properties) {
        this.uploadDirectory = properties.directory();
        this.maxImageBytes = properties.maxImageSize().toBytes();
        this.thumbnailSize = properties.thumbnailSize();
        this.maxSourceDimension = properties.maxSourceDimension();
    }

    /**
     * 保存一张图片并生成列表用缩略图。
     *
     * @param file 客户端上传的 multipart 文件
     * @return 原图和缩略图的相对访问地址
     */
    public FileUploadResponse storeImage(MultipartFile file) {
        validateFile(file);
        ImageType imageType = detectImageType(file);
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String originalName = fileId + imageType.extension;
        String thumbnailName = fileId + "_thumb.jpg";
        Path originalPath = resolveUploadPath(originalName);
        Path thumbnailPath = resolveUploadPath(thumbnailName);

        try {
            Files.createDirectories(uploadDirectory);
            file.transferTo(originalPath);
            createThumbnail(originalPath, thumbnailPath);
        } catch (BusinessException exception) {
            deleteQuietly(originalPath, thumbnailPath);
            throw exception;
        } catch (IOException exception) {
            deleteQuietly(originalPath, thumbnailPath);
            throw new IllegalStateException("图片保存失败", exception);
        }

        log.info("图片上传成功: fileId={}", fileId);
        return new FileUploadResponse(
                "/uploads/" + originalName,
                "/uploads/" + thumbnailName
        );
    }

    /**
     * 判断地址是否为本服务上传的原始图片且文件仍然存在。
     *
     * @param url 图片相对地址
     * @return 地址合法且文件存在时返回 true
     */
    public boolean isOriginalImageAvailable(String url) {
        if (url == null || !ORIGINAL_IMAGE_URL_PATTERN.matcher(url).matches()) {
            return false;
        }
        String fileName = url.substring("/uploads/".length());
        return Files.isRegularFile(resolveUploadPath(fileName));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要上传的图片");
        }
        if (file.getSize() > maxImageBytes) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片不能超过 2MB");
        }
    }

    private ImageType detectImageType(MultipartFile file) {
        byte[] header;
        try (InputStream inputStream = file.getInputStream()) {
            header = inputStream.readNBytes(FILE_HEADER_LENGTH);
        } catch (IOException exception) {
            throw new IllegalStateException("读取图片失败", exception);
        }

        if (isJpeg(header)) {
            return ImageType.JPEG;
        }
        if (isPng(header)) {
            return ImageType.PNG;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "只允许上传 JPG 或 PNG 图片");
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] header) {
        byte[] pngSignature = {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };
        return header.length >= pngSignature.length
                && Arrays.equals(
                Arrays.copyOf(header, pngSignature.length),
                pngSignature
        );
    }

    private void createThumbnail(Path originalPath, Path thumbnailPath) throws IOException {
        BufferedImage sourceImage = readImage(originalPath);
        int sourceMaxDimension = Math.max(sourceImage.getWidth(), sourceImage.getHeight());
        double scale = Math.min(1.0, (double) thumbnailSize / sourceMaxDimension);
        int thumbnailWidth = Math.max(1, (int) Math.round(sourceImage.getWidth() * scale));
        int thumbnailHeight = Math.max(1, (int) Math.round(sourceImage.getHeight() * scale));

        BufferedImage thumbnail = new BufferedImage(
                thumbnailWidth,
                thumbnailHeight,
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = thumbnail.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, thumbnailWidth, thumbnailHeight);
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            graphics.drawImage(sourceImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
        } finally {
            graphics.dispose();
        }

        if (!ImageIO.write(thumbnail, "jpg", thumbnailPath.toFile())) {
            throw new IOException("缺少 JPEG 图片编码器");
        }
    }

    private BufferedImage readImage(Path originalPath) {
        try (ImageInputStream input = ImageIO.createImageInputStream(originalPath.toFile())) {
            if (input == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件无法解析");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件无法解析");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件无法解析");
                }
                if (Math.max(width, height) > maxSourceDimension) {
                    throw new BusinessException(
                            ErrorCode.BAD_REQUEST,
                            "图片单边不能超过 " + maxSourceDimension + "px"
                    );
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件无法解析");
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件无法解析");
        }
    }

    private Path resolveUploadPath(String fileName) {
        Path target = uploadDirectory.resolve(fileName).normalize();
        if (!target.startsWith(uploadDirectory)) {
            throw new IllegalStateException("非法上传路径");
        }
        return target;
    }

    private void deleteQuietly(Path... paths) {
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException exception) {
                log.warn("清理上传文件失败: fileName={}", path.getFileName());
            }
        }
    }

    private enum ImageType {
        JPEG(".jpg"),
        PNG(".png");

        private final String extension;

        ImageType(String extension) {
            this.extension = extension;
        }
    }
}
