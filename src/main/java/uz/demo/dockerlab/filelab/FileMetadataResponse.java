package uz.demo.dockerlab.filelab;

import java.time.Instant;
import java.util.UUID;

public record FileMetadataResponse(
        UUID id,
        String originalName,
        String contentType,
        long sizeBytes,
        Instant uploadedAt
) {
    public static FileMetadataResponse from(StoredFile file) {
        return new FileMetadataResponse(
                file.getId(),
                file.getOriginalName(),
                file.getContentType(),
                file.getSizeBytes(),
                file.getUploadedAt()
        );
    }
}