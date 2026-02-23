package ru.stroy1click.confirmationcode.client.impl;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.web.client.ResourceAccessException;
import ru.stroy1click.common.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.stroy1click.confirmationcode.client.UserClient;
import ru.stroy1click.confirmationcode.dto.UserDto;
import ru.stroy1click.common.util.ValidationErrorUtils;

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
    public UserDto getByEmail(String email) {
        log.info("getUserByEmail {}", email);
        try {
            return this.restClient.get()
                    .uri("?email={email}", email)
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
