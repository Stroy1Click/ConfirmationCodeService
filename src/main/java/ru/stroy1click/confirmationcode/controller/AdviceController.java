package ru.stroy1click.confirmationcode.controller;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.stroy1click.common.exception.*;

import java.util.Locale;

@RestControllerAdvice
@RequiredArgsConstructor
public class AdviceController {

    private final MessageSource messageSource;

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleException(NotFoundException exception){
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, this.messageSource.getMessage(
                        exception.getMessageKey(),
                        exception.getArgs(),
                        Locale.getDefault()
                )
        );
        problemDetail.setTitle(
                this.messageSource.getMessage(
                        "error.title.not_found",
                        null,
                        Locale.getDefault()
                )
        );
        return problemDetail;
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleException(ValidationException exception){
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        if(exception.isRawMessage()) {
            problemDetail.setDetail(exception.getMessage());
        } else {
            problemDetail.setDetail(
                    this.messageSource.getMessage(
                            exception.getMessageKey(),
                            exception.getArgs(),
                            Locale.getDefault()
                    )
            );
        }

        problemDetail.setTitle(
                this.messageSource.getMessage(
                        "error.title.validation",
                        null,
                        Locale.getDefault()
                )
        );
        return problemDetail;
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ProblemDetail handleException(RequestNotPermitted exception){
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                this.messageSource.getMessage(
                        "error.details.too_many_requests",
                        null,
                        Locale.getDefault()
                )
        );
        problemDetail.setTitle(this.messageSource.getMessage(
                "error.title.too_many_requests",
                null,
                Locale.getDefault()
        ));
        return problemDetail;
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ProblemDetail handleException(CallNotPermittedException exception){
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                this.messageSource.getMessage(
                        "error.details.service_unavailable",
                        null,
                        Locale.getDefault()
                )
        );
        problemDetail.setTitle(this.messageSource.getMessage(
                "error.title.service_unavailable",
                null,
                Locale.getDefault()
        ));
        return problemDetail;
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ProblemDetail handleException(ServiceUnavailableException exception){
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                this.messageSource.getMessage(
                        "error.details.service_unavailable",
                        null,
                        Locale.getDefault()
                )
        );
        problemDetail.setTitle(this.messageSource.getMessage(
                "error.title.service_unavailable",
                null,
                Locale.getDefault()
        ));
        return problemDetail;
    }
}
