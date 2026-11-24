package uz.demo.dockerlab.filelab;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "lab.file-upload",
        name = "enabled",
        havingValue = "true"
)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${lab.storage.endpoint}") String endpoint,
            @Value("${lab.storage.access-key}") String accessKey,
            @Value("${lab.storage.secret-key}") String secretKey
    ) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}