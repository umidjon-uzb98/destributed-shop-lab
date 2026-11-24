package uz.demo.dockerlab.filelab;

import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@ConditionalOnProperty(
        prefix = "lab.file-upload",
        name = "enabled",
        havingValue = "true"
)
public class FileStorageService {

    private final MinioClient minioClient;
    private final StoredFileRepository repository;
    private final String bucket;

    public FileStorageService(
            MinioClient minioClient,
            StoredFileRepository repository,
            @Value("${lab.storage.bucket}") String bucket
    ) {
        this.minioClient = minioClient;
        this.repository = repository;
        this.bucket = bucket;
    }

    public FileUploadResponse upload(MultipartFile file) {

        if (file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Empty file is not allowed"
            );
        }

        if (file.getSize() > 2L * 1024 * 1024) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "File size exceeds 2MB"
            );
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            originalName = "unnamed";
        }

        // Client file nomiga ishonib key yasamaymiz.
        String objectKey = "uploads/" + UUID.randomUUID();

        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucket)
                            .build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucket)
                                .build()
                );
            }

            // 1. File content -> MinIO
            try (var inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectKey)
                                .stream(
                                        inputStream,
                                        file.getSize(),
                                        -1
                                )
                                .contentType(contentType)
                                .build()
                );
            }

            // 2. File metadata -> PostgreSQL
            StoredFile storedFile = new StoredFile(
                    originalName,
                    objectKey,
                    bucket,
                    contentType,
                    file.getSize()
            );

            StoredFile saved = repository.save(storedFile);

            return new FileUploadResponse(
                    saved.getId(),
                    originalName,
                    objectKey,
                    bucket,
                    file.getSize()
            );

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "File storage operation failed",
                    ex
            );
        }
    }
}