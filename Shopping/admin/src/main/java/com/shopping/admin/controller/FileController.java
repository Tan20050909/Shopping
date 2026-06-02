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
import java.util.List;
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

    // 扩展名对应的允许 Content-Type
    private static final Map<String, List<String>> EXT_CONTENT_TYPES = Map.ofEntries(
            Map.entry(".jpg", List.of("image/jpeg")),
            Map.entry(".jpeg", List.of("image/jpeg")),
            Map.entry(".png", List.of("image/png")),
            Map.entry(".gif", List.of("image/gif")),
            Map.entry(".webp", List.of("image/webp")),
            Map.entry(".bmp", List.of("image/bmp")),
            Map.entry(".pdf", List.of("application/pdf")),
            Map.entry(".doc", List.of("application/msword")),
            Map.entry(".docx", List.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            Map.entry(".xls", List.of("application/vnd.ms-excel")),
            Map.entry(".xlsx", List.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            Map.entry(".ppt", List.of("application/vnd.ms-powerpoint")),
            Map.entry(".pptx", List.of("application/vnd.openxmlformats-officedocument.presentationml.presentation")),
            Map.entry(".mp4", List.of("video/mp4")),
            Map.entry(".mp3", List.of("audio/mpeg")),
            Map.entry(".wav", List.of("audio/wav"))
    );

    private void validateContentType(String ext, String contentType) {
        List<String> allowedTypes = EXT_CONTENT_TYPES.get(ext);
        if (allowedTypes == null) {
            return; // 未知扩展名，由白名单拦截
        }
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessException(400, "无法识别文件类型");
        }
        // 取分号前的主体类型（忽略 charset 等参数）
        String mainType = contentType.contains(";") ? contentType.substring(0, contentType.indexOf(";")).trim() : contentType.trim();
        if (!allowedTypes.contains(mainType)) {
            throw new BusinessException(400, "文件内容类型与扩展名不匹配");
        }
    }

    /** 将相对路径 uploadPath 转为基于项目根目录的绝对路径 */
    private Path resolveUploadDir() {
        Path dir = Paths.get(uploadPath);
        if (!dir.isAbsolute()) {
            dir = Paths.get(System.getProperty("user.dir")).resolve(dir).normalize();
        }
        return dir;
    }

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
        validateContentType(ext, file.getContentType());
        String newFilename = UUID.randomUUID().toString().replace("-", "") + ext;
        String datePath = java.time.LocalDate.now().toString().replace("-", "/");
        Path baseDir = resolveUploadDir();
        Path dirPath = baseDir.resolve(datePath);
        Files.createDirectories(dirPath);
        Path filePath = dirPath.resolve(newFilename);
        file.transferTo(filePath.toFile());
        String url = accessPath + "/" + datePath + "/" + newFilename;
        return Result.success(Map.of("url", url, "filename", originalFilename != null ? originalFilename : newFilename));
    }

    @PostMapping("/upload-batch")
    public Result<?> uploadBatch(@RequestParam("files") MultipartFile[] files) throws IOException {
        java.util.List<Map<String, String>> results = new java.util.ArrayList<>();
        Path baseDir = resolveUploadDir();
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
                validateContentType(ext, file.getContentType());
                String newFilename = UUID.randomUUID().toString().replace("-", "") + ext;
                String datePath = java.time.LocalDate.now().toString().replace("-", "/");
                Path dirPath = baseDir.resolve(datePath);
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
