package ru.stroy1click.confirmationcode.controller;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.stroy1click.common.exception.ValidationException;
import ru.stroy1click.confirmationcode.dto.CodeVerificationRequest;
import ru.stroy1click.confirmationcode.dto.CreateConfirmationCodeRequest;
import ru.stroy1click.confirmationcode.dto.UpdatePasswordRequest;
import ru.stroy1click.confirmationcode.service.ConfirmationCodeService;
import ru.stroy1click.common.util.ValidationErrorUtils;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/confirmation-codes")
@RequiredArgsConstructor
@RateLimiter(name = "confirmationCodeLimiter")
@Tag(name = "ConfirmationCode Controller", description = "Создание и отправка кода подтверждения пользователю")
public class ConfirmationCodeController {

    private final ConfirmationCodeService confirmationCodeService;

    private final MessageSource messageSource;

    @PostMapping("/email/verify")
    @Operation(summary = "Подтверждение почты пользователя")
    public ResponseEntity<String> verifyEmail(@RequestBody @Valid CodeVerificationRequest codeVerificationRequest,
                                          BindingResult bindingResult){
        if(bindingResult.hasFieldErrors()) throw new ValidationException(ValidationErrorUtils.collectErrorsToString(
                bindingResult.getFieldErrors()
        ));

        this.confirmationCodeService.verifyEmail(codeVerificationRequest);

        return ResponseEntity.ok(
                this.messageSource.getMessage(
                        "info.confirmation_code.email.confirmed",
                        null,
                        Locale.getDefault()
                )
        );
    }

    @PostMapping("/password-reset")
    @Operation(summary = "Обновление пароля")
    public ResponseEntity<String> updatePassword(@RequestBody @Valid UpdatePasswordRequest updatePasswordRequest,
                                                 BindingResult bindingResult){
        if(bindingResult.hasFieldErrors()) throw new ValidationException(ValidationErrorUtils.collectErrorsToString(
                bindingResult.getFieldErrors()
        ));

        this.confirmationCodeService.updatePassword(updatePasswordRequest);

        return ResponseEntity.ok(
                this.messageSource.getMessage(
                        "info.password.successfully_updated",
                        null,
                        Locale.getDefault()
                )
        );
    }

    @PostMapping("/regeneration")
    @Operation(summary = "Пересоздание кода подтверждения и отправка его по почте пользователю")
    public ResponseEntity<String> recreate(@RequestBody @Valid CreateConfirmationCodeRequest codeRequest,
                                           BindingResult bindingResult){
        if(bindingResult.hasFieldErrors()) throw new ValidationException(ValidationErrorUtils.collectErrorsToString(
                bindingResult.getFieldErrors()
        ));

       this.confirmationCodeService.recreate(codeRequest);

        return ResponseEntity.ok(
                this.messageSource.getMessage(
                        "info.confirmation_code.sent",
                        null,
                        Locale.getDefault()
                )
        );
    }

    @PostMapping
    @Operation(summary = "Создать новый код подтверждения")
    public ResponseEntity<String> create(@RequestBody @Valid CreateConfirmationCodeRequest codeRequest,
                                         BindingResult bindingResult){
        if(bindingResult.hasFieldErrors()) throw new ValidationException(ValidationErrorUtils.collectErrorsToString(
                bindingResult.getFieldErrors()
        ));

        this.confirmationCodeService.create(codeRequest);

        return ResponseEntity.ok(
                this.messageSource.getMessage(
                        "info.confirmation_code.sent",
                        null,
                        Locale.getDefault()
                )
        );
    }
}
