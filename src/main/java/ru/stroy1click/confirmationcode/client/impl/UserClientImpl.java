package ru.stroy1click.confirmationcode.client.impl;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.web.client.ResourceAccessException;
import ru.stroy1click.confirmationcode.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.stroy1click.confirmationcode.client.UserClient;
import ru.stroy1click.confirmationcode.dto.UserDto;
import ru.stroy1click.confirmationcode.dto.ConfirmEmailRequest;
import ru.stroy1click.confirmationcode.dto.UserServiceUpdatePasswordRequest;
import ru.stroy1click.confirmationcode.util.ValidationErrorUtils;

@Slf4j
@Service
@CircuitBreaker(name = "userClient")
public class UserClientImpl implements UserClient {

    private final RestClient restClient;

    public UserClientImpl(@Value(value = "${url.user}") String url){
        this.restClient = RestClient.builder()
                .baseUrl(url)
                .build();
    }

    @Override
    public void updateEmailConfirmedStatus(ConfirmEmailRequest email) {
        log.info("updateEmailConfirmedStatus {}", email);
        try {
            this.restClient.patch()
                    .uri("/email-status")
                    .body(email)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,(request, response) -> {
                        ValidationErrorUtils.validateStatus(response);
                    })
                    .body(String.class);
        }  catch (ResourceAccessException e){
            log.error("updateEmailConfirmedStatus error ", e);
            throw new ServiceUnavailableException();
        }
    }

    @Override
    public void updatePassword(UserServiceUpdatePasswordRequest updatePasswordRequest) {
        log.info("updatePassword {}", updatePasswordRequest);
        try {
            this.restClient.patch()
                    .uri("/password")
                    .body(updatePasswordRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,(request, response) -> {
                        ValidationErrorUtils.validateStatus(response);
                    })
                    .body(String.class);
        } catch (ResourceAccessException e){
            log.error("updatePassword error ", e);
            throw new ServiceUnavailableException();
        }
    }

    @Override
    public UserDto getByEmail(String email) {
        log.info("getUserByEmail {}", email);
        try {
            return this.restClient.get()
                    .uri("/email?email={email}", email)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,(request, response) -> {
                        ValidationErrorUtils.validateStatus(response);
                    })
                    .body(UserDto.class);
        } catch (ResourceAccessException e){
            log.error("getUserByEmail error", e);
            throw new ServiceUnavailableException();
        }
    }

}
