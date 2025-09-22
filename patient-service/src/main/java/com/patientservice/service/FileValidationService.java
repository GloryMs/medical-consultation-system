package com.patientservice.service;

import com.commonlibrary.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class FileValidationService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB in bytes
    private static final int MAX_FILES_PER_CASE = 10;
    
    // Allowed MIME types for PDFs and images
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "application/pdf",
        "image/jpeg",
        "image/jpg", 
        "image/png",
        "image/gif",
        "image/bmp",
        "image/webp"
    );
    
    // Allowed file extensions as backup check
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".pdf", ".PDF",
        ".jpg", ".JPG", ".jpeg", ".JPEG",
        ".png", ".PNG",
        ".gif", ".GIF",
        ".bmp", ".BMP",
        ".webp", ".WEBP"
    );

    /**
     * Validate files for case submission
     */
    public void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return; // Files are optional
        }

        // Check file count
        if (files.size() > MAX_FILES_PER_CASE) {
            throw new BusinessException(
                String.format("Maximum %d files allowed per case. You uploaded %d files.", 
                    MAX_FILES_PER_CASE, files.size()), 
                HttpStatus.BAD_REQUEST
            );
        }

        // Validate each file
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            validateSingleFile(file, i + 1);
        }

        log.info("File validation completed successfully for {} files", files.size());
    }

    /**
     * Validate a single file
     */
    private void validateSingleFile(MultipartFile file, int fileNumber) {
        if (file.isEmpty()) {
            throw new BusinessException(
                String.format("File #%d is empty", fileNumber), 
                HttpStatus.BAD_REQUEST
            );
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(
                String.format("File #%d '%s' exceeds maximum size of 10MB (%.2fMB)", 
                    fileNumber, file.getOriginalFilename(), file.getSize() / (1024.0 * 1024.0)), 
                HttpStatus.BAD_REQUEST
            );
        }

        // Check MIME type
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new BusinessException(
                String.format("File #%d '%s' has unsupported format (%s). Only PDF and image files are allowed.", 
                    fileNumber, file.getOriginalFilename(), mimeType), 
                HttpStatus.BAD_REQUEST
            );
        }

        // Check file extension as backup
        String fileName = file.getOriginalFilename();
        if (fileName == null || !hasValidExtension(fileName)) {
            throw new BusinessException(
                String.format("File #%d '%s' has unsupported extension. Only PDF and image files are allowed.", 
                    fileNumber, fileName), 
                HttpStatus.BAD_REQUEST
            );
        }

        log.debug("File validation passed for: {} (Size: {}KB, Type: {})", 
            fileName, file.getSize() / 1024, mimeType);
    }

    /**
     * Check if file has valid extension
     */
    private boolean hasValidExtension(String fileName) {
        return ALLOWED_EXTENSIONS.stream()
                .anyMatch(ext -> fileName.toLowerCase().endsWith(ext.toLowerCase()));
    }

    /**
     * Get file extension from filename
     */
    public String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * Check if file is an image
     */
    public boolean isImageFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * Check if file is a PDF
     */
    public boolean isPdfFile(String mimeType) {
        return "application/pdf".equals(mimeType);
    }

    // Add these methods to FileValidationService.java

    /**
     * Validate files for case update (considering existing files)
     */
    public void validateFilesForCaseUpdate(List<MultipartFile> newFiles, int existingFileCount) {
        if (newFiles == null || newFiles.isEmpty()) {
            throw new BusinessException("No files provided for upload", HttpStatus.BAD_REQUEST);
        }

        // Check total file count including existing files
        int totalFileCount = existingFileCount + newFiles.size();
        if (totalFileCount > MAX_FILES_PER_CASE) {
            throw new BusinessException(
                    String.format("Total files would exceed maximum limit. Current files: %d, " +
                                    "New files: %d, Maximum allowed: %d",
                            existingFileCount, newFiles.size(), MAX_FILES_PER_CASE),
                    HttpStatus.BAD_REQUEST
            );
        }

        // Validate each new file
        for (int i = 0; i < newFiles.size(); i++) {
            MultipartFile file = newFiles.get(i);
            validateSingleFile(file, i + 1);
        }

        log.info("File validation completed for case update: {} new files, {} existing files",
                newFiles.size(), existingFileCount);
    }

    /**
     * Get remaining file slots for a case
     */
    public int getRemainingFileSlots(int existingFileCount) {
        return Math.max(0, MAX_FILES_PER_CASE - existingFileCount);
    }

    /**
     * Check if case can accept more files
     */
    public boolean canAcceptMoreFiles(int existingFileCount) {
        return existingFileCount < MAX_FILES_PER_CASE;
    }

    /**
     * Validate files can be added to case
     */
    public void validateCanAddFiles(int existingFileCount, int newFileCount) {
        if (!canAcceptMoreFiles(existingFileCount)) {
            throw new BusinessException(
                    String.format("Case already has maximum number of files (%d)", MAX_FILES_PER_CASE),
                    HttpStatus.BAD_REQUEST
            );
        }

        if (existingFileCount + newFileCount > MAX_FILES_PER_CASE) {
            int remainingSlots = getRemainingFileSlots(existingFileCount);
            throw new BusinessException(
                    String.format("Cannot upload %d files. Only %d slots remaining (maximum %d files per case)",
                            newFileCount, remainingSlots, MAX_FILES_PER_CASE),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}