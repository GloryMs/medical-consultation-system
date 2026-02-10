package com.doctorservice.service;

import com.doctorservice.entity.DoctorDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for storing and retrieving doctor verification documents
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorDocumentFileStorageService {
    @Value("${app.file.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.file.base-url:http://172.16.1.122:8083/api/files}")
    private String baseUrl;

    private final DoctorFileEncryptionService encryptionService;

    /**
     * Store document file with encryption and compression
     */
    public String storeFile(MultipartFile file, Long doctorId, DoctorDocument.DocumentType documentType) throws Exception {
        // Create directory structure: uploads/doctors/{doctorId}/{documentType}/{year}/{month}/
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String relativePath = String.format("doctors/%d/%s/%s", doctorId, documentType.name(), datePrefix);

        Path uploadPath = Paths.get(uploadDir, relativePath);
        Files.createDirectories(uploadPath);

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + extension + ".enc"; // .enc for encrypted

        Path filePath = uploadPath.resolve(uniqueFilename);

        // Save compressed and encrypted file
        encryptionService.saveEncryptedFile(file.getBytes(), filePath.toString());

        // Return relative path for storage in database
        String storedPath = relativePath + "/" + uniqueFilename;
        log.info("Document stored successfully: {} -> {}", originalFilename, storedPath);

        return storedPath;
    }

    /**
     * Retrieve and decrypt file
     */
    public byte[] retrieveFile(String storedPath) throws Exception {
        Path filePath = Paths.get(uploadDir, storedPath);

        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + storedPath);
        }

        byte[] decryptedData = encryptionService.readDecryptedFile(filePath.toString());
        log.info("Document retrieved and decrypted: {}", storedPath);

        return decryptedData;
    }

    /**
     * Delete stored file
     */
    public void deleteFile(String storedPath) throws IOException {
        Path filePath = Paths.get(uploadDir, storedPath);

        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("Document deleted: {}", storedPath);
        } else {
            log.warn("Attempted to delete non-existent file: {}", storedPath);
        }
    }

    /**
     * Get file size
     */
    public long getFileSize(String storedPath) throws IOException {
        Path filePath = Paths.get(uploadDir, storedPath);

        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + storedPath);
        }

        return Files.size(filePath);
    }

    /**
     * Generate file URL for access
     */
    public String generateFileUrl(Long documentId) {
        return String.format("%s/documents/%d", baseUrl, documentId);
    }

    /**
     * Generate download URL
     */
    public String generateDownloadUrl(Long documentId) {
        return String.format("%s/documents/%d/download", baseUrl, documentId);
    }

    /**
     * Calculate SHA-256 checksum for file integrity
     */
    public String calculateChecksum(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Validate file type
     */
    public boolean isValidFileType(String mimeType) {
        return mimeType != null && (
                mimeType.equals("application/pdf") ||
                        mimeType.equals("image/jpeg") ||
                        mimeType.equals("image/jpg") ||
                        mimeType.equals("image/png")
        );
    }

    /**
     * Validate file size (max 5MB)
     */
    public boolean isValidFileSize(long size) {
        long maxSize = 5 * 1024 * 1024; // 5MB in bytes
        return size > 0 && size <= maxSize;
    }
}
