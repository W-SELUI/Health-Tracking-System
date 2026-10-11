package com.group15.health_tracking_system.user.entity;

import com.group15.health_tracking_system.common.model.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "provider_verifications")
public class ProviderVerification extends BaseEntity {

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "provider_id",
            nullable = false,
            unique = true
    )
    private User provider;

    @Column(
            name = "original_file_name",
            nullable = false,
            length = 255
    )
    private String originalFileName;

    @Column(
            name = "file_type",
            nullable = false,
            length = 100
    )
    private String fileType;

    @Column(
            name = "file_path",
            nullable = false,
            length = 500
    )
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private VerificationStatus status =
            VerificationStatus.PENDING;

    @Column(
            name = "admin_note",
            length = 500
    )
    private String adminNote;

    @Column(
            name = "verified_at"
    )
    private LocalDateTime verifiedAt;

    public ProviderVerification() {
    }

    public User getProvider() {
        return provider;
    }

    public void setProvider(
            User provider) {

        this.provider = provider;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(
            String originalFileName) {

        this.originalFileName =
                originalFileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(
            String fileType) {

        this.fileType = fileType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(
            String filePath) {

        this.filePath = filePath;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(
            VerificationStatus status) {

        this.status = status;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(
            String adminNote) {

        this.adminNote = adminNote;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(
            LocalDateTime verifiedAt) {

        this.verifiedAt = verifiedAt;
    }
}