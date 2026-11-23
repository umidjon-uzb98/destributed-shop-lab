
package uz.demo.dockerlab.filelab;

import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/file-lab")
@ConditionalOnProperty(
        prefix = "lab.file-upload",
        name = "enabled",
        havingValue = "true"
)
public class FileUploadController {

    private final FileValidationService service;

    public FileUploadController(
            FileValidationService service
    ) {
        this.service = service;
    }

    @PostMapping(
            value = "/inspect",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public FileValidationService.UploadResult inspect(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        return service.validate(file);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooLarge() {

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of(
                        "error",
                        "Multipart upload size limit exceeded"
                ));
    }
}
