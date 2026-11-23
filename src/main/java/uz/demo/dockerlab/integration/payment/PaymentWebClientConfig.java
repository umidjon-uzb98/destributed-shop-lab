
package uz.demo.dockerlab.integration.payment;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class PaymentWebClientConfig {

    @Bean("paymentWebClient")
    public WebClient paymentWebClient(
            WebClient.Builder builder,
            @Value("${payment.base-url}")
            String baseUrl
    ) {

        HttpClient httpClient = HttpClient.create()
                .disableRetry(true)
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        1000
                )
                .responseTimeout(
                        Duration.ofSeconds(2)
                );

        return builder
                .baseUrl(baseUrl)
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .clientConnector(
                        new ReactorClientHttpConnector(
                                httpClient
                        )
                )
                .build();
    }
}
