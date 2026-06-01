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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        return saveFile(file, "images", request);
    }

    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadVideo(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        return saveFile(file, "videos", request);
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        return saveFile(file, "files", request);
    }

    private Map<String, Object> saveFile(MultipartFile file, String folder, HttpServletRequest request) throws IOException {
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0 && dot < originalName.length() - 1) {
            ext = originalName.substring(dot);
        }

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
