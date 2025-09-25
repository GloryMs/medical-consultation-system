package com.patientservice.service;

import com.patientservice.entity.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    @Value("${app.file.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.file.base-url:http://localhost:8082/api/files}")
    private String baseUrl;

    private final FileEncryptionService encryptionService;

    /**
     * Store file with encryption and compression
     */
    public String storeFile(MultipartFile file, Long caseId) throws Exception {
        // Create directory structure: uploads/cases/{caseId}/{year}/{month}/
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String relativePath = String.format("cases/%d/%s", caseId, datePrefix);
        
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
        log.info("File stored successfully: {} -> {}", originalFilename, storedPath);
        
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
        log.info("File retrieved and decrypted: {}", storedPath);

        System.out.println("Retrieving File: " + storedPath);
        System.out.println("Decrypted File size: " + decryptedData.length);


        return decryptedData;
    }

    /**
     * Delete stored file
     */
    public boolean deleteFile(String storedPath) {
        try {
            Path filePath = Paths.get(uploadDir, storedPath);
            boolean deleted = Files.deleteIfExists(filePath);
            
            if (deleted) {
                log.info("File deleted successfully: {}", storedPath);
            } else {
                log.warn("File not found for deletion: {}", storedPath);
            }
            
            return deleted;
        } catch (IOException e) {
            log.error("Error deleting file: {}", storedPath, e);
            return false;
        }
    }

    /**
     * Get file size in bytes
     */
    public long getFileSize(String storedPath) throws IOException {
        Path filePath = Paths.get(uploadDir, storedPath);
        return Files.size(filePath);
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String storedPath) {
        Path filePath = Paths.get(uploadDir, storedPath);
        return Files.exists(filePath);
    }

    /**
     * Generate public URL for file access
     */
    public String generateFileUrl(Long documentId) {
        return String.format("%s/%d", baseUrl, documentId);
    }

    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Clean up old files (utility method for scheduled cleanup)
     */
    public void cleanupOldFiles(int daysOld) {
        // Implementation for cleaning up files older than specified days
        // This would be called by a scheduled task
        log.info("File cleanup initiated for files older than {} days", daysOld);
    }
}