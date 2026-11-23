
package uz.demo.dockerlab.integration.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class PaymentClientConfig {

    @Bean("paymentRestClient")
    public RestClient paymentRestClient(
            @Value("${payment.base-url}")
            String baseUrl
    ) {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(
                Duration.ofSeconds(1)
        );

        factory.setReadTimeout(
                Duration.ofSeconds(2)
        );

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
