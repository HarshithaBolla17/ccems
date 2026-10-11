package com.ccems.service;

import com.ccems.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Saves uploaded files under a single folder with random names, so a file name sent by a user
 * can never choose where the file goes. Only a short list of document and image types is accepted.
 */
@Service
public class FileStorageService {

    public static final long MAX_BYTES = 10L * 1024 * 1024;

    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("txt", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("md", "text/plain"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("zip", "application/zip"));

    public record Stored(String storedName, String originalName, long size) {}

    private final Path root;

    public FileStorageService(@Value("${ccems.upload-dir:uploads}") String dir) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public Stored store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("Choose a file to upload.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "That file is too big. The limit is 10 MB.");
        }
        String original = cleanName(file.getOriginalFilename());
        String ext = extension(original);
        if (!TYPES.containsKey(ext)) {
            throw BusinessException.badRequest(
                    "That file type isn't allowed. Use PDF, images, Word, Excel, PowerPoint, text, CSV or ZIP.");
        }
        String stored = UUID.randomUUID() + "." + ext;
        Path target = root.resolve(stored).normalize();
        if (!target.startsWith(root)) {
            throw BusinessException.badRequest("Invalid file name.");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "The file could not be saved. Try again.");
        }
        return new Stored(stored, original, file.getSize());
    }

    public Resource load(String storedName) {
        try {
            Path path = root.resolve(storedName).normalize();
            if (!path.startsWith(root) || !Files.exists(path)) {
                throw BusinessException.notFound("The file is no longer available.");
            }
            return new UrlResource(path.toUri());
        } catch (IOException e) {
            throw BusinessException.notFound("The file is no longer available.");
        }
    }

    public void delete(String storedName) {
        if (storedName == null) return;
        try {
            Path path = root.resolve(storedName).normalize();
            if (path.startsWith(root)) Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A leftover file is harmless; the database row is what matters.
        }
    }

    public MediaType mediaType(String fileName) {
        String type = TYPES.get(extension(fileName));
        return type == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(type);
    }

    private static String cleanName(String name) {
        String n = name == null ? "file" : name.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).trim();
        if (n.length() > 150) n = n.substring(n.length() - 150);
        return n.isEmpty() ? "file" : n;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
