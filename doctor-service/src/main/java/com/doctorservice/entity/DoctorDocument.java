package com.doctorservice.entity;

import com.commonlibrary.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing documents uploaded by doctors for verification
 * Includes medical licenses, certificates, and experience documents
 */
@Entity
@Table(name = "doctor_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    @JsonBackReference
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "original_file_size")
    private Double originalFileSize; // in bytes

    @Column(name = "stored_file_size")
    private Double storedFileSize; // in bytes (after compression/encryption)

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "is_encrypted", nullable = false)
    private Boolean isEncrypted = true;

    @Column(name = "is_compressed", nullable = false)
    private Boolean isCompressed = true;

    @Column(name = "checksum")
    private String checksum; // SHA-256 checksum for integrity

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "verified_by_admin", nullable = false)
    private Boolean verifiedByAdmin = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private Long verifiedBy; // Admin user ID who verified

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    /**
     * Document Type Enum
     */
    public enum DocumentType {
        LICENSE,      // Medical License (Required)
        CERTIFICATE,  // Professional Certificate (Required)
        EXPERIENCE    // Experience Documents (Optional)
    }

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}