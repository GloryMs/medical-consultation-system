package com.patientservice.entity;

import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.DocumentType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    @JsonBackReference
    private Case medicalCase;

    @Column(nullable = false)
    private Long uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, name = "file_url")
    private String fileUrl; // This will store the relative path to encrypted file

    @Column(name = "original_file_size")
    private Double originalFileSize; // Size before compression

    @Column(name = "stored_file_size")
    private Double storedFileSize; // Size after compression and encryption

    @Column(name = "mime_type")
    private String mimeType;

    @Column(nullable = false)
    private Boolean isVerified = false;

    @Column(name = "is_encrypted")
    private Boolean isEncrypted = true; // New files will always be encrypted

    @Column(name = "is_compressed")
    private Boolean isCompressed = true; // New files will always be compressed

    private String description;

    @Column(name = "access_url")
    private String accessUrl; // Public URL for file access

    @Column(name = "checksum")
    private String checksum; // For file integrity verification
}