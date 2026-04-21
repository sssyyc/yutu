package com.yutu.modules.system.controller;

import com.yutu.common.exception.BizException;
import com.yutu.common.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {
    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择要上传的图片");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BizException(400, "仅支持上传图片文件");
        }

        Path rootPath = resolveUploadRoot();
        try {
            Files.createDirectories(rootPath);
            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String filename = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                    + "-" + UUID.randomUUID().toString().replace("-", "");
            if (StringUtils.hasText(extension)) {
                filename = filename + "." + extension.toLowerCase(Locale.ROOT);
            }
            Path target = rootPath.resolve(filename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(filename)
                    .toUriString();
            Map<String, String> result = new HashMap<>();
            result.put("name", filename);
            result.put("url", url);
            return Result.ok(result);
        } catch (IOException e) {
            throw new BizException(500, "图片上传失败");
        }
    }

    private Path resolveUploadRoot() {
        Set<Path> candidates = new LinkedHashSet<>();
        candidates.add(Paths.get(uploadDir).toAbsolutePath().normalize());
        candidates.add(Paths.get("yutu-admin", uploadDir).toAbsolutePath().normalize());

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }
}
