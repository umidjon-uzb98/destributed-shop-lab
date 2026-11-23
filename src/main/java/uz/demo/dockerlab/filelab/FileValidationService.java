
package uz.demo.dockerlab.filelab;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;

@Service
public class FileValidationService {

    private static final long MAX_BYTES = 2 * 1024 * 1024;

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
    };

    public UploadResult validate(MultipartFile file)
            throws IOException {

        // 1. Bo'sh faylni rad etamiz
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File must not be empty"
            );
        }

        // 2. Fayl hajmini tekshiramiz
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Maximum file size is 2 MB"
            );
        }

        // 3. Client yuborgan filename'ni tekshiramiz
        String filename = file.getOriginalFilename();

        if (filename == null ||
                !filename.matches(
                        "[A-Za-z0-9][A-Za-z0-9._-]{0,119}"
                )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid filename"
            );
        }

        // 4. Extension'ni aniqlaymiz
        int dot = filename.lastIndexOf('.');

        String extension = dot < 0
                ? ""
                : filename.substring(dot + 1)
                .toLowerCase(Locale.ROOT);

        String expectedType = switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only PNG and JPEG are allowed"
            );
        };

        // 5. Client bildirgan MIME type
        if (!expectedType.equalsIgnoreCase(
                file.getContentType()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "File MIME type does not match extension"
            );
        }

        // 6. Fayl boshidagi 8 byte'ni o'qiymiz
        byte[] header;

        try (InputStream stream = file.getInputStream()) {
            header = stream.readNBytes(8);
        }

        // 7. File signature
        boolean validSignature;

        if ("png".equals(extension)) {

            validSignature = Arrays.equals(
                    header,
                    PNG_SIGNATURE
            );

        } else {

            validSignature =
                    header.length >= 3
                            && (header[0] & 0xFF) == 0xFF
                            && (header[1] & 0xFF) == 0xD8
                            && (header[2] & 0xFF) == 0xFF;
        }

        if (!validSignature) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "File signature is invalid"
            );
        }

        // 8. Validation muvaffaqiyatli
        return new UploadResult(
                filename,
                file.getSize(),
                expectedType,
                "VALIDATED_NOT_STORED"
        );
    }

    public record UploadResult(
            String originalFilename,
            long sizeBytes,
            String acceptedMediaType,
            String status
    ) {}
}
