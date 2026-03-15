package com.trinetra.service;

import com.trinetra.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final Path uploadRoot;
    private final String storageBaseUrl;

    public FileStorageService(
            @Value("${app.upload.dir}") String uploadDir,
            @Value("${STORAGE_URL:}") String storageBaseUrl
    ) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.storageBaseUrl = storageBaseUrl;
    }

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not initialize file storage", ex);
        }
    }

    public List<String> storeFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<String> storedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "evidence" : file.getOriginalFilename());
            if (originalName.contains("..")) {
                throw new BadRequestException("Invalid file name: " + originalName);
            }

            String extension = "";
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalName.substring(dotIndex);
            }
            String storedName = UUID.randomUUID() + extension;

            try {
                Files.copy(file.getInputStream(), uploadRoot.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
                storedFiles.add(storedName);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to store uploaded file", ex);
            }
        }
        return storedFiles;
    }

    public Map<String, String> storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Evidence file is required");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "evidence" : file.getOriginalFilename());
        if (originalName.contains("..")) {
            throw new BadRequestException("Invalid file name: " + originalName);
        }

        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }
        String storedName = UUID.randomUUID() + extension;

        try {
            Files.copy(file.getInputStream(), uploadRoot.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store uploaded file", ex);
        }

        String publicUrl = buildPublicUrl(storedName);
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        return Map.of(
                "fileUrl", publicUrl,
                "fileType", contentType
        );
    }

    public String buildPublicUrl(String storedName) {
        if (StringUtils.hasText(storageBaseUrl)) {
            String base = storageBaseUrl.endsWith("/") ? storageBaseUrl.substring(0, storageBaseUrl.length() - 1) : storageBaseUrl;
            return base + "/uploads/" + storedName;
        }
        return "/uploads/" + storedName;
    }
}