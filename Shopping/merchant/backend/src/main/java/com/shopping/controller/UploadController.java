package com.shopping.controller;

import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    // 图片白名单：jpg、jpeg、png、webp、gif
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif"
    );
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB

    // 视频白名单：mp4、webm
    private static final Set<String> VIDEO_CONTENT_TYPES = Set.of(
            "video/mp4", "video/webm"
    );
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            ".mp4", ".webm"
    );
    private static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024; // 50MB

    // 危险类型黑名单
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            ".svg", ".html", ".htm", ".js", ".jsx", ".ts", ".tsx",
            ".zip", ".rar", ".7z", ".exe", ".sh", ".bat", ".cmd",
            ".php", ".asp", ".aspx", ".jsp"
    );

    // 普通文件白名单及对应 Content-Type
    private static final Set<String> FILE_EXTENSIONS = Set.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".mp3", ".wav"
    );
    private static final Map<String, List<String>> FILE_CONTENT_TYPES = Map.ofEntries(
            Map.entry(".pdf", List.of("application/pdf")),
            Map.entry(".doc", List.of("application/msword")),
            Map.entry(".docx", List.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            Map.entry(".xls", List.of("application/vnd.ms-excel")),
            Map.entry(".xlsx", List.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            Map.entry(".ppt", List.of("application/vnd.ms-powerpoint")),
            Map.entry(".pptx", List.of("application/vnd.openxmlformats-officedocument.presentationml.presentation")),
            Map.entry(".mp3", List.of("audio/mpeg")),
            Map.entry(".wav", List.of("audio/wav"))
    );

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        validateImage(file);
        return saveFile(file, "images", request);
    }

    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadVideo(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        validateVideo(file);
        return saveFile(file, "videos", request);
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        validateFile(file);
        return saveFile(file, "files", request);
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("图片大小不能超过10MB");
        }
        String contentType = file.getContentType();
        String ext = getExtension(file.getOriginalFilename());

        // 检查扩展名是否在危险黑名单中
        if (DANGEROUS_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型：" + ext);
        }

        // 同时检查 Content-Type 和扩展名
        if (!IMAGE_CONTENT_TYPES.contains(contentType) || !IMAGE_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("仅支持 jpg/jpeg/png/webp/gif 图片");
        }
    }

    private void validateVideo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new IllegalArgumentException("视频大小不能超过50MB");
        }
        String contentType = file.getContentType();
        String ext = getExtension(file.getOriginalFilename());

        // 检查扩展名是否在危险黑名单中
        if (DANGEROUS_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型：" + ext);
        }

        // 同时检查 Content-Type 和扩展名
        if (!VIDEO_CONTENT_TYPES.contains(contentType) || !VIDEO_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("仅支持 mp4/webm 视频");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过10MB");
        }
        String ext = getExtension(file.getOriginalFilename());

        // 检查扩展名是否在危险黑名单中
        if (DANGEROUS_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型：" + ext);
        }

        // 检查扩展名是否在白名单中
        if (ext.isEmpty() || !FILE_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型，允许的类型：" + String.join(", ", FILE_EXTENSIONS));
        }

        // 校验 Content-Type 与扩展名匹配
        String contentType = file.getContentType();
        List<String> allowedTypes = FILE_CONTENT_TYPES.get(ext);
        if (allowedTypes != null) {
            if (contentType == null || contentType.isBlank()) {
                throw new IllegalArgumentException("无法识别文件类型");
            }
            String mainType = contentType.contains(";") ? contentType.substring(0, contentType.indexOf(";")).trim() : contentType.trim();
            if (!allowedTypes.contains(mainType)) {
                throw new IllegalArgumentException("文件内容类型与扩展名不匹配");
            }
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    private Map<String, Object> saveFile(MultipartFile file, String folder, HttpServletRequest request) throws IOException {
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = getExtension(originalName);

        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = Paths.get(uploadDir, folder);
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        file.transferTo(target.toFile());

        String path = "/uploads/" + folder + "/" + filename;
        String host = request == null ? "" : request.getServerName();
        if ("localhost".equalsIgnoreCase(host)) {
            host = "127.0.0.1";
        }
        String baseUrl = request == null ? "" : request.getScheme() + "://" + host + ":" + request.getServerPort();

        Map<String, Object> res = new HashMap<>();
        res.put("url", baseUrl.isBlank() ? path : baseUrl + path);
        res.put("path", path);
        res.put("name", originalName);
        return res;
    }
}
