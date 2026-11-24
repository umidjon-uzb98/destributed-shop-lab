package uz.demo.dockerlab.filelab;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/storage-lab/files")
@ConditionalOnProperty(
        prefix = "lab.file-upload",
        name = "enabled",
        havingValue = "true"
)
public class FileDownloadController {

    private final StoredFileRepository repository;
    private final MinioClient minioClient;

    public FileDownloadController(
            StoredFileRepository repository,
            MinioClient minioClient
    ) {
        this.repository = repository;
        this.minioClient = minioClient;
    }

    // 1. File metadata
    @GetMapping("/{id}")
    public FileMetadataResponse metadata(
            @PathVariable UUID id
    ) {
        StoredFile file = findFile(id);

        return FileMetadataResponse.from(file);
    }

    // 2. Streaming download
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable UUID id
    ) {
        StoredFile file = findFile(id);

        try {
            GetObjectResponse stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(file.getBucket())
                            .object(file.getObjectKey())
                            .build()
            );

            ContentDisposition disposition =
                    ContentDisposition.attachment()
                            .filename(
                                    file.getOriginalName(),
                                    StandardCharsets.UTF_8
                            )
                            .build();

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            disposition.toString()
                    )
                    .contentType(
                            MediaType.APPLICATION_OCTET_STREAM
                    )
                    .contentLength(file.getSizeBytes())
                    .body(new InputStreamResource(stream));

        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to retrieve file from object storage",
                    ex
            );
        }
    }

    private StoredFile findFile(UUID id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "File metadata not found"
                        )
                );
    }
}