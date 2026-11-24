package uz.demo.dockerlab.filelab;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/storage-lab")
@ConditionalOnProperty(
        prefix = "lab.file-upload",
        name = "enabled",
        havingValue = "true"
)
public class StorageLabController {

    private final FileStorageService storageService;

    public StorageLabController(
            FileStorageService storageService
    ) {
        this.storageService = storageService;
    }

    @PostMapping(
            value = "/files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<FileUploadResponse> upload(
            @RequestParam("file") MultipartFile file
    ) {
        FileUploadResponse result =
                storageService.upload(file);

        return ResponseEntity
                .status(201)
                .body(result);
    }
}