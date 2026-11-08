package com.group15.health_tracking_system.user.service;

import com.group15.health_tracking_system.user.entity.ProviderVerification;
import com.group15.health_tracking_system.user.entity.VerificationStatus;
import com.group15.health_tracking_system.user.repository.ProviderVerificationRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProviderVerificationService {

    private final ProviderVerificationRepository repository;

    public ProviderVerificationService(
            ProviderVerificationRepository repository) {

        this.repository = repository;
    }

    /*
     * Upload or replace a provider licence.
     */
    @Transactional
    public ProviderVerification uploadLicence(
            com.group15.health_tracking_system.user.entity.User provider,
            MultipartFile file) {

        if (provider == null
                || provider.getId() == null) {

            throw new IllegalArgumentException(
                    "A valid provider account is required.");
        }

        if (file == null
                || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Please select a licence file to upload.");
        }

        validateFile(file);

        try {

            String originalFileName =
                    file.getOriginalFilename();

            String extension =
                    getExtension(
                            originalFileName);

            String safeFileName =
                    "provider_"
                            + provider.getId()
                            + "_"
                            + UUID.randomUUID()
                            + extension;

            byte[] fileBytes =
                    file.getBytes();

            ProviderVerification verification =
                    repository
                            .findByProvider_Id(
                                    provider.getId())
                            .orElseGet(
                                    ProviderVerification::new);

            /*
             * If replacing an old licence,
             * remove the old physical file.
             */
            if (verification.getId() != null
                    && verification.getFilePath() != null
                    && !verification.getFilePath().isBlank()
                && !verification.getFilePath().startsWith("database:")) {

                deleteOldFile(
                        verification.getFilePath());
            }

            verification.setProvider(
                    provider);

            verification.setOriginalFileName(
                    originalFileName);

            String contentType =
                    file.getContentType();

            if (contentType == null
                    || contentType.isBlank()) {

                contentType =
                        "application/octet-stream";
            }

            verification.setFileType(
                    contentType);

            /*
             * The file is kept in the database. file_path now only
             * records a reference name for older records and audits.
             */
            verification.setFilePath(
                    "database:" + safeFileName);

            verification.setFileData(
                    fileBytes);

            /*
             * Every new or replacement licence
             * must be reviewed by an administrator.
             */
            verification.setStatus(
                    VerificationStatus.PENDING);

            verification.setAdminNote(
                    null);

            verification.setVerifiedAt(
                    null);

            return repository.save(
                    verification);

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "The licence file could not be saved.",
                    exception);
        }
    }

    /*
     * Get one provider's verification.
     */
    public Optional<ProviderVerification>
            getByProviderId(
                    Long providerId) {

        return repository
                .findByProvider_Id(
                        providerId);
    }

    /*
     * Get one verification by its ID.
     */
    public ProviderVerification getById(
            Long id) {

        return repository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Provider verification record not found."));
    }

    /*
     * Get all provider verification records.
     */
    public List<ProviderVerification> getAll() {

        return repository
                .findAllByOrderByCreatedAtDesc();
    }

    /*
     * Get pending verification records.
     */
    public List<ProviderVerification> getPending() {

        return repository
                .findByStatusOrderByCreatedAtDesc(
                        VerificationStatus.PENDING);
    }

    /*
     * Load the uploaded licence file.
     */
    public Resource loadLicenceFile(
            Long verificationId) {

        ProviderVerification verification =
                getById(
                        verificationId);

        if (verification.getFileData() != null) {
            return new org.springframework.core.io.ByteArrayResource(
                    verification.getFileData());
        }

        // Older records saved before licences moved into the database.
        try {

            Path filePath =
                    Paths.get(
                            verification.getFilePath());

            Resource resource =
                    new UrlResource(
                            filePath.toUri());

            if (!resource.exists()
                    || !resource.isReadable()) {

                throw new IllegalStateException(
                        "Licence file could not be found.");
            }

            return resource;

        } catch (MalformedURLException exception) {

            throw new IllegalStateException(
                    "Licence file could not be opened.",
                    exception);
        }
    }

    /*
     * Admin verifies a provider.
     */
    @Transactional
    public ProviderVerification verifyProvider(
            Long verificationId,
            String adminNote) {

        ProviderVerification verification =
                getById(
                        verificationId);

        verification.setStatus(
                VerificationStatus.VERIFIED);

        verification.setAdminNote(
                cleanNote(
                        adminNote));

        verification.setVerifiedAt(
                LocalDateTime.now());

        return repository.save(
                verification);
    }

    /*
     * Admin rejects a provider licence.
     */
    @Transactional
    public ProviderVerification rejectProvider(
            Long verificationId,
            String adminNote) {

        ProviderVerification verification =
                getById(
                        verificationId);

        verification.setStatus(
                VerificationStatus.REJECTED);

        verification.setAdminNote(
                cleanNote(
                        adminNote));

        verification.setVerifiedAt(
                null);

        return repository.save(
                verification);
    }

    /*
     * Validate supported licence files.
     */
    private void validateFile(
            MultipartFile file) {

        String originalFileName =
                file.getOriginalFilename();

        if (originalFileName == null
                || originalFileName.isBlank()) {

            throw new IllegalArgumentException(
                    "The uploaded file must have a file name.");
        }

        String extension =
                getExtension(
                        originalFileName)
                        .toLowerCase();

        boolean validExtension =
                extension.equals(".pdf")
                        || extension.equals(".jpg")
                        || extension.equals(".jpeg")
                        || extension.equals(".png");

        if (!validExtension) {

            throw new IllegalArgumentException(
                    "Only PDF, JPG, JPEG and PNG licence files are allowed.");
        }

        // 5 MB keeps each stored file within hosted database row limits.
        long maximumSize =
                5L * 1024L * 1024L;

        if (file.getSize() > maximumSize) {

            throw new IllegalArgumentException(
                    "Licence file must not exceed 5 MB.");
        }
    }

    private String getExtension(
            String fileName) {

        int dotIndex =
                fileName.lastIndexOf('.');

        if (dotIndex < 0) {
            return "";
        }

        return fileName.substring(
                dotIndex);
    }

    private String cleanNote(
            String adminNote) {

        if (adminNote == null) {
            return null;
        }

        String cleaned =
                adminNote.trim();

        return cleaned.isEmpty()
                ? null
                : cleaned;
    }

    private void deleteOldFile(
            String oldFilePath) {

        try {

            Files.deleteIfExists(
                    Paths.get(
                            oldFilePath));

        } catch (IOException ignored) {

            /*
             * Failure to remove an old file
             * should not stop a new upload.
             */
        }
    }
}