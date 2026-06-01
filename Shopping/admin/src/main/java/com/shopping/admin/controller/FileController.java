package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    @Value("${file.access-path:/uploads}")
    private String accessPath;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".mp4", ".mp3", ".wav"
    );

    // 危险类型黑名单（双重校验）
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            ".svg", ".html", ".htm", ".js", ".jsx", ".ts", ".tsx",
            ".zip", ".rar", ".7z", ".exe", ".sh", ".bat", ".cmd",
            ".php", ".asp", ".aspx", ".jsp"
    );

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "文件大小不能超过10MB");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : "";

        // 双重校验：检查危险类型黑名单
        if (DANGEROUS_EXTENSIONS.contains(ext)) {
            throw new BusinessException(400, "不支持的文件类型：" + ext);
        }

        if (ext.isEmpty() || !ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(400, "不支持的文件类型，允许的类型：" + String.join(", ", ALLOWED_EXTENSIONS));
        }
        String newFilename = UUID.randomUUID().toString().replace("-", "") + ext;
        String datePath = java.time.LocalDate.now().toString().replace("-", "/");
        Path dirPath = Paths.get(uploadPath, datePath);
        Files.createDirectories(dirPath);
        Path filePath = dirPath.resolve(newFilename);
        file.transferTo(filePath.toFile());
        String url = accessPath + "/" + datePath + "/" + newFilename;
        return Result.success(Map.of("url", url, "filename", originalFilename != null ? originalFilename : newFilename));
    }

    @PostMapping("/upload-batch")
    public Result<?> uploadBatch(@RequestParam("files") MultipartFile[] files) throws IOException {
        java.util.List<Map<String, String>> results = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                if (file.getSize() > MAX_FILE_SIZE) {
                    throw new BusinessException(400, "文件大小不能超过10MB: " + file.getOriginalFilename());
                }
                String originalFilename = file.getOriginalFilename();
                String ext = originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                        : "";

                // 双重校验：检查危险类型黑名单
                if (DANGEROUS_EXTENSIONS.contains(ext)) {
                    throw new BusinessException(400, "不支持的文件类型：" + ext);
                }

                if (ext.isEmpty() || !ALLOWED_EXTENSIONS.contains(ext)) {
                    throw new BusinessException(400, "不支持的文件类型: " + originalFilename);
                }
                String newFilename = UUID.randomUUID().toString().replace("-", "") + ext;
                String datePath = java.time.LocalDate.now().toString().replace("-", "/");
                Path dirPath = Paths.get(uploadPath, datePath);
                Files.createDirectories(dirPath);
                Path filePath = dirPath.resolve(newFilename);
                file.transferTo(filePath.toFile());
                String url = accessPath + "/" + datePath + "/" + newFilename;
                results.add(Map.of("url", url, "filename", originalFilename != null ? originalFilename : newFilename));
            }
        }
        return Result.success(results);
    }
}
