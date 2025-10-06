package com.doctorservice.service;

import com.doctorservice.config.PdfConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Autowired
    private PdfConfig pdfConfig;

    @Autowired
    private FileEncryptionService fileEncryptionService;

    /**
     * Save PDF file to storage with encryption and compression
     * @param content PDF content as byte array
     * @param filename Original filename
     * @return Accessible URL of the saved file
     */
    public String saveFile(byte[] content, String filename, Long doctorId,
                           Long patientId, Long caseId) {
        try {
            // Validate file size
            double fileSizeMB = content.length / (1024.0 * 1024.0);
            if (fileSizeMB > pdfConfig.getMaxSizeMb()) {
                throw new RuntimeException(
                        String.format("File size %.2f MB exceeds maximum allowed size of %d MB",
                                fileSizeMB, pdfConfig.getMaxSizeMb())
                );
            }

            // Generate unique filename with timestamp
            String uniqueFilename = generateUniqueFilename(filename);

            // Create subdirectories based on date (YYYY/MM/DD)
            Path dateBasedPath = createDateBasedDirectory(doctorId, patientId, caseId);

            // Full path for the file
            Path targetLocation = dateBasedPath.resolve(uniqueFilename);

            // Encrypt and compress the content
            log.info("Encrypting and compressing file: {}", uniqueFilename);
            byte[] encryptedData = fileEncryptionService.compressAndEncrypt(content);

            // Write encrypted file to disk
            Files.write(targetLocation, encryptedData);

            double compressedSizeMB = encryptedData.length / (1024.0 * 1024.0);
            log.info("File saved successfully: {} (Original: {:.2f} MB, Encrypted: {:.2f} MB)",
                    targetLocation, fileSizeMB, compressedSizeMB);

            // Generate accessible URL
            String relativeUrl = getRelativePathFromBase(targetLocation);
            return pdfConfig.getBaseUrl() + "/" + relativeUrl;

        } catch (Exception ex) {
            log.error("Failed to save file: {}", filename, ex);
            throw new RuntimeException("Failed to save file: " + filename, ex);
        }
    }

    /**
     * Save file with custom path
     */
    public String saveFileWithPath(byte[] content, String relativePath, String filename) {
        try {
            Path basePath = getFileStorageLocation();
            Path customPath = basePath.resolve(relativePath);
            ensureDirectoryExists(customPath);

            Path targetLocation = customPath.resolve(filename);

            // Encrypt and compress the content
            byte[] encryptedData = fileEncryptionService.compressAndEncrypt(content);

            // Write encrypted file to disk
            Files.write(targetLocation, encryptedData);

            log.info("File saved at custom path: {}", targetLocation);

            String relativeUrl = getRelativePathFromBase(targetLocation);
            return pdfConfig.getBaseUrl() + "/" + relativeUrl;

        } catch (Exception ex) {
            log.error("Failed to save file with custom path", ex);
            throw new RuntimeException("Failed to save file with custom path", ex);
        }
    }

    /**
     * Get file as byte array (decrypted and decompressed)
     */
    public byte[] getFile(String fileUrl) {
        try {
            // Extract relative path from URL
            String relativePath = fileUrl.replace(pdfConfig.getBaseUrl() + "/", "");
            Path basePath = getFileStorageLocation();
            Path filePath = basePath.resolve(relativePath).normalize();

            // Security check: ensure file is within storage directory
            if (!filePath.startsWith(basePath)) {
                throw new SecurityException("Attempted path traversal attack detected");
            }

            File file = filePath.toFile();
            if (!file.exists()) {
                throw new RuntimeException("File not found: " + relativePath);
            }

            // Read encrypted file
            byte[] encryptedData = Files.readAllBytes(filePath);

            // Decrypt and decompress
            log.info("Decrypting and decompressing file: {}", relativePath);
            byte[] decryptedData = fileEncryptionService.decryptAndDecompress(encryptedData);

            return decryptedData;

        } catch (Exception ex) {
            log.error("Failed to read file: {}", fileUrl, ex);
            throw new RuntimeException("Failed to read file", ex);
        }
    }

    /**
     * Delete file
     */
    public boolean deleteFile(String fileUrl) {
        try {
            String relativePath = fileUrl.replace(pdfConfig.getBaseUrl() + "/", "");
            Path basePath = getFileStorageLocation();
            Path filePath = basePath.resolve(relativePath).normalize();

            if (!filePath.startsWith(basePath)) {
                throw new SecurityException("Attempted path traversal attack detected");
            }

            File file = filePath.toFile();
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    log.info("File deleted successfully: {}", relativePath);
                }
                return deleted;
            }

            log.warn("File not found for deletion: {}", relativePath);
            return false;

        } catch (Exception ex) {
            log.error("Failed to delete file: {}", fileUrl, ex);
            return false;
        }
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String fileUrl) {
        try {
            String relativePath = fileUrl.replace(pdfConfig.getBaseUrl() + "/", "");
            Path basePath = getFileStorageLocation();
            Path filePath = basePath.resolve(relativePath).normalize();
            return filePath.toFile().exists();
        } catch (Exception ex) {
            log.error("Error checking file existence: {}", fileUrl, ex);
            return false;
        }
    }

    /**
     * Get file size in bytes (encrypted size)
     */
    public long getFileSize(String fileUrl) {
        try {
            String relativePath = fileUrl.replace(pdfConfig.getBaseUrl() + "/", "");
            Path basePath = getFileStorageLocation();
            Path filePath = basePath.resolve(relativePath).normalize();
            return Files.size(filePath);
        } catch (IOException ex) {
            log.error("Error getting file size: {}", fileUrl, ex);
            return 0L;
        }
    }

    /**
     * Clean up old temp files (can be scheduled)
     */
    public void cleanupTempFiles(int olderThanHours) {
        try {
            Path tempPath = getTempStorageLocation();
            File tempDir = tempPath.toFile();

            if (tempDir.exists() && tempDir.isDirectory()) {
                long cutoffTime = System.currentTimeMillis() - (olderThanHours * 3600000L);

                File[] files = tempDir.listFiles();
                if (files != null) {
                    int deletedCount = 0;
                    for (File file : files) {
                        if (file.lastModified() < cutoffTime) {
                            if (file.delete()) {
                                deletedCount++;
                            }
                        }
                    }
                    log.info("Cleaned up {} temp files older than {} hours",
                            deletedCount, olderThanHours);
                }
            }
        } catch (Exception ex) {
            log.error("Error during temp files cleanup", ex);
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Get file storage location and ensure it exists
     */
    private Path getFileStorageLocation() {
        try {
            Path path = Paths.get(pdfConfig.getStorage().getBasePath())
                    .toAbsolutePath()
                    .normalize();
            ensureDirectoryExists(path);
            return path;
        } catch (IOException ex) {
            throw new RuntimeException("Could not create file storage directory", ex);
        }
    }

    /**
     * Get temp storage location and ensure it exists
     */
    private Path getTempStorageLocation() {
        try {
            Path path = Paths.get(pdfConfig.getStorage().getTempDir())
                    .toAbsolutePath()
                    .normalize();
            ensureDirectoryExists(path);
            return path;
        } catch (IOException ex) {
            throw new RuntimeException("Could not create temp storage directory", ex);
        }
    }

    /**
     * Ensure directory exists, create if not
     */
    private void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            log.debug("Created directory: {}", directory);
        }
    }

    private String generateUniqueFilename(String originalFilename) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        // Extract file extension
        String extension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalFilename.substring(lastDot);
        }

        // Remove extension from original name
        String nameWithoutExt = lastDot > 0 ?
                originalFilename.substring(0, lastDot) : originalFilename;

        // Sanitize filename
        nameWithoutExt = nameWithoutExt.replaceAll("[^a-zA-Z0-9_-]", "_");

        return String.format("%s_%s_%s%s", nameWithoutExt, timestamp, uuid, extension);
    }

    private Path createDateBasedDirectory( Long doctorId, Long patientId, Long caseId)
            throws IOException {
        LocalDateTime now = LocalDateTime.now();
        String year = String.valueOf(now.getYear());
        String doctor = String.format("doctor_" + doctorId.toString());
        String patient = String.format("patient_" + patientId.toString());
        String medicalCase = String.format("case_" + caseId.toString());

        Path basePath = getFileStorageLocation();
        Path dateBasedPath = basePath.resolve(year)
                .resolve(doctor)
                .resolve(patient)
                .resolve(medicalCase);

        ensureDirectoryExists(dateBasedPath);
        return dateBasedPath;
    }

    private String getRelativePathFromBase(Path absolutePath) {
        Path basePath = getFileStorageLocation();
        return basePath.relativize(absolutePath).toString()
                .replace("\\", "/"); // Ensure forward slashes for URLs
    }
}