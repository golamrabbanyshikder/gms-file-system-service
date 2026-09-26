package com.gms.filesystem.repository;

import com.gms.filesystem.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    List<FileMetadata> findByFileGroupIdOrderByVersionDesc(String fileGroupId);

    Optional<FileMetadata> findTopByFileGroupIdOrderByVersionDesc(String fileGroupId);
}
