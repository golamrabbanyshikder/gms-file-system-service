package com.gms.filesystem.controller;

import com.gms.filesystem.entity.FileMetadata;
import com.gms.filesystem.exception.FileStorageException;
import com.gms.filesystem.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileGroupId", required = false) String fileGroupId,
            @RequestParam(value = "relatedEntityType", required = false) String relatedEntityType,
            @RequestParam(value = "relatedEntityId", required = false) Long relatedEntityId) {
        try {
            FileMetadata metadata = fileStorageService.uploadFile(file, fileGroupId, relatedEntityType, relatedEntityId);
            return ResponseEntity.status(HttpStatus.CREATED).body(metadata);
        } catch (FileStorageException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadFile(@PathVariable Long id) {
        try {
            FileStorageService.FileDownload download = fileStorageService.downloadFile(id);
            FileMetadata metadata = download.metadata();
            MediaType mediaType = metadata.getContentType() != null
                    ? MediaType.parseMediaType(metadata.getContentType())
                    : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getOriginalFileName() + "\"")
                    .contentType(mediaType)
                    .body(download.data());
        } catch (FileStorageException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id) {
        try {
            fileStorageService.deleteFile(id);
            return ResponseEntity.ok(new ErrorResponse("File deleted successfully"));
        } catch (FileStorageException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/versions/{fileGroupId}")
    public ResponseEntity<List<FileMetadata>> getVersions(@PathVariable String fileGroupId) {
        return ResponseEntity.ok(fileStorageService.getVersions(fileGroupId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMetadata(@PathVariable Long id) {
        try {
            FileMetadata metadata = fileStorageService.getMetadata(id);
            return ResponseEntity.ok(metadata);
        } catch (FileStorageException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    public static class ErrorResponse {
        public String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
