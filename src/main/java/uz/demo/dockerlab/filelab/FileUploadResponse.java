package uz.demo.dockerlab.filelab;

import java.util.UUID;

public record FileUploadResponse(
        UUID id,
        String originalName,
        String objectKey,
        String bucket,
        long sizeBytes
) {
}