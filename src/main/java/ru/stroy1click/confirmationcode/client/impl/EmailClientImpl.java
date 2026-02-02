package ru.stroy1click.confirmationcode.client.impl;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import ru.stroy1click.confirmationcode.client.EmailClient;
import ru.stroy1click.confirmationcode.exception.ServiceUnavailableException;
import ru.stroy1click.confirmationcode.dto.SendEmailRequest;
import ru.stroy1click.confirmationcode.util.ValidationErrorUtils;

@Slf4j
@Service
@CircuitBreaker(name = "emailClient")
public class EmailClientImpl implements EmailClient {

    private final RestClient restClient;

    public EmailClientImpl(@Value(value = "${url.email}") String url){
        this.restClient = RestClient.builder()
                .baseUrl(url)
                .build();
    }

    @Override
    @Async("asyncTaskExecutor")
    public void sendEmail(SendEmailRequest sendEmailRequest) {
        log.info("sendEmail {}", sendEmailRequest);
        try {
            this.restClient.post()
                    .uri("/send")
                    .body(sendEmailRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,(request, response) -> {
                        ValidationErrorUtils.validateStatus(response);
                    })
                    .body(String.class);
        } catch (ResourceAccessException e){
            log.error("sendEmail error ", e);
            throw new ServiceUnavailableException();
        }
    }

}
