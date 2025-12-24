package com.doctorservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.doctorservice.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for serving encrypted PDF report files
 * Files are automatically decrypted and decompressed before serving
 */
@RestController
@RequestMapping("/api/files/reports")
@Slf4j
public class ReportFileController {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Serve/view PDF report file (inline - opens in browser)
     * File is automatically decrypted and decompressed
     * Example: GET /api/files/reports/2024/10/02/medical_report_case50_report1_20241002_143000_abc123.pdf
     */
    @GetMapping("/serve/**")
    public ResponseEntity<Resource> serveFile(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            HttpServletRequest request) {
        try {
            // Get the full request URL
            String requestUrl = request.getRequestURL().toString();

            log.info("Serving PDF file request: {}", requestUrl);
            //http://172.16.1.122:8081/api/files/reports/serve/2025/doctor_4/patient_17/case_43/medical_report_case43_report6_20251007_105454_20251007_110140_0a955355.pdf
            //http://172.16.1.122:8081/api/files/reports
            // Get file content (will be automatically decrypted and decompressed)
            requestUrl=requestUrl.replace("server/","");
            byte[] fileContent = fileStorageService.getFile(requestUrl);

            // Create resource
            ByteArrayResource resource = new ByteArrayResource(fileContent);

            // Extract filename from URL
            String[] pathParts = requestUrl.split("/");
            String filename = pathParts[pathParts.length - 1];

            // Set headers for inline viewing (opens in browser)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(fileContent.length);
            headers.setContentDispositionFormData("inline", filename);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            log.info("Successfully served decrypted PDF file: {} (Size: {} bytes)", filename, fileContent.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (SecurityException ex) {
            ex.printStackTrace();
            log.error("Security violation while serving file: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Error serving PDF file: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Download PDF report file (forces download instead of inline view)
     * File is automatically decrypted and decompressed
     * Example: GET /api/files/reports/download/2024/10/02/medical_report_case50_report1_20241002_143000_abc123.pdf
     */
    @GetMapping("/download/**")
    public ResponseEntity<Resource> downloadFile(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            HttpServletRequest request) {

        try {
            // Get the full request URL
            String requestUrl = request.getRequestURL().toString();

            // Remove /download from the URL to get the actual file URL
            String fileUrl = requestUrl.replace("/download", "");

            log.info("Downloading PDF file request: {}", fileUrl);

            // Get file content (will be automatically decrypted and decompressed)
            byte[] fileContent = fileStorageService.getFile(fileUrl);

            // Create resource
            ByteArrayResource resource = new ByteArrayResource(fileContent);

            // Extract filename from URL
            String[] pathParts = fileUrl.split("/");
            String filename = pathParts[pathParts.length - 1];

            // Set headers for download (forces save dialog)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(fileContent.length);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            log.info("Successfully prepared decrypted PDF file for download: {} (Size: {} bytes)",
                    filename, fileContent.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (SecurityException ex) {
            log.error("Security violation while downloading file: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (Exception ex) {
            log.error("Error downloading PDF file: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Check if file exists
     * Does not decrypt the file, just checks existence
     */
    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkFileExists(
            @RequestParam String fileUrl) {

        try {
            boolean exists = fileStorageService.fileExists(fileUrl);
            log.info("File existence check for {}: {}", fileUrl, exists);
            return ResponseEntity.ok(ApiResponse.success(exists));

        } catch (Exception ex) {
            log.error("Error checking file existence: {}", ex.getMessage(), ex);
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }

    /**
     * Get file metadata (size, etc.)
     * Returns encrypted file size, not decrypted size
     */
    @GetMapping("/metadata")
    public ResponseEntity<ApiResponse<FileMetadata>> getFileMetadata(
            @RequestParam String fileUrl) {

        try {
            boolean exists = fileStorageService.fileExists(fileUrl);
            if (!exists) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("File not found", HttpStatus.BAD_REQUEST));
            }

            long encryptedSize = fileStorageService.getFileSize(fileUrl);
            String filename = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

            FileMetadata metadata = new FileMetadata();
            metadata.setFilename(filename);
            metadata.setFileUrl(fileUrl);
            metadata.setEncryptedSizeBytes(encryptedSize);
            metadata.setEncryptedSizeMB(encryptedSize / (1024.0 * 1024.0));
            metadata.setExists(true);
            metadata.setEncrypted(true);
            metadata.setCompressed(true);

            log.info("File metadata retrieved for: {} (Encrypted size: {} bytes)", filename, encryptedSize);

            return ResponseEntity.ok(ApiResponse.success(metadata));

        } catch (Exception ex) {
            log.error("Error getting file metadata: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error getting file metadata", HttpStatus.BAD_REQUEST));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("File service is operational"));
    }

    // ========== RESPONSE DTOs ==========

    /**
     * File metadata response
     */
    @Data
    public static class FileMetadata {
        private String filename;
        private String fileUrl;
        private long encryptedSizeBytes;
        private double encryptedSizeMB;
        private boolean exists;
        private boolean encrypted;
        private boolean compressed;
    }
}