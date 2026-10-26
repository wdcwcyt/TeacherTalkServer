package com.zl.teacherttalk.controller;

import com.zl.teacherttalk.common.Result;
import com.zl.teacherttalk.dto.FileUploadResponse;
import com.zl.teacherttalk.service.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileUploadResponse> upload(
            @RequestPart("file") MultipartFile file
    ) {
        return Result.success(fileStorageService.storeImage(file));
    }
}
