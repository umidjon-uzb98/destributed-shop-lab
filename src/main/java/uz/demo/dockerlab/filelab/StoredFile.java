package uz.demo.dockerlab.filelab;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "stored_files")
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false, unique = true)
    private String objectKey;

    @Column(nullable = false)
    private String bucket;

    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private Instant uploadedAt;

    protected StoredFile() {
    }

    public StoredFile(
            String originalName,
            String objectKey,
            String bucket,
            String contentType,
            long sizeBytes
    ) {
        this.originalName = originalName;
        this.objectKey = objectKey;
        this.bucket = bucket;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = Instant.now();
    }

}