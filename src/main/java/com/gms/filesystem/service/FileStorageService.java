package com.gms.filesystem.service;

import com.gms.filesystem.entity.FileMetadata;
import com.gms.filesystem.exception.FileStorageException;
import com.gms.filesystem.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "docx");
    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024; // 20MB

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    public FileMetadata uploadFile(MultipartFile file, String fileGroupId, String relatedEntityType, Long relatedEntityId)
            throws FileStorageException {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new FileStorageException("File exceeds maximum allowed size of 20MB");
        }

        String originalFileName = file.getOriginalFilename();
        String extension = extractExtension(originalFileName);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new FileStorageException("Unsupported file type: " + extension);
        }

        String groupId = fileGroupId;
        int newVersion = 1;
        if (groupId == null || groupId.isBlank()) {
            groupId = UUID.randomUUID().toString();
        } else {
            newVersion = fileMetadataRepository.findTopByFileGroupIdOrderByVersionDesc(groupId)
                    .map(latest -> latest.getVersion() + 1)
                    .orElse(1);
        }

        String storedFileName = UUID.randomUUID().toString() + "." + extension.toLowerCase();

        try {
            Path uploadBaseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadBaseDir)) {
                Files.createDirectories(uploadBaseDir);
            }

            Path filePath = uploadBaseDir.resolve(storedFileName);
            file.transferTo(filePath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file", e);
        }

        FileMetadata metadata = new FileMetadata();
        metadata.setFileGroupId(groupId);
        metadata.setVersion(newVersion);
        metadata.setOriginalFileName(originalFileName);
        metadata.setStoredFileName(storedFileName);
        metadata.setContentType(file.getContentType());
        metadata.setFileSizeBytes(file.getSize());
        metadata.setRelatedEntityType(relatedEntityType);
        metadata.setRelatedEntityId(relatedEntityId);
        metadata.setCreatedAt(LocalDateTime.now());

        return fileMetadataRepository.save(metadata);
    }

    public FileDownload downloadFile(Long metadataId) throws FileStorageException {
        FileMetadata metadata = getMetadata(metadataId);

        Path uploadBaseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path resolvedPath = uploadBaseDir.resolve(metadata.getStoredFileName()).normalize();

        if (!resolvedPath.startsWith(uploadBaseDir)) {
            throw new FileStorageException("Invalid file path detected");
        }

        try {
            if (!Files.exists(resolvedPath)) {
                throw new FileStorageException("File not found on disk: id=" + metadataId);
            }
            byte[] data = Files.readAllBytes(resolvedPath);
            return new FileDownload(data, metadata);
        } catch (IOException e) {
            throw new FileStorageException("Failed to download file", e);
        }
    }

    public void deleteFile(Long metadataId) throws FileStorageException {
        FileMetadata metadata = getMetadata(metadataId);

        Path uploadBaseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path resolvedPath = uploadBaseDir.resolve(metadata.getStoredFileName()).normalize();

        try {
            Files.deleteIfExists(resolvedPath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file", e);
        }

        fileMetadataRepository.delete(metadata);
    }

    public List<FileMetadata> getVersions(String fileGroupId) {
        return fileMetadataRepository.findByFileGroupIdOrderByVersionDesc(fileGroupId);
    }

    public FileMetadata getMetadata(Long metadataId) throws FileStorageException {
        return fileMetadataRepository.findById(metadataId)
                .orElseThrow(() -> new FileStorageException("File not found: id=" + metadataId));
    }

    private String extractExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(dotIndex + 1);
    }

    public record FileDownload(byte[] data, FileMetadata metadata) {
    }
}
