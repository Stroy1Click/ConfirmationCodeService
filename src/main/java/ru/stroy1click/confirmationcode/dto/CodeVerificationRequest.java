package ru.stroy1click.confirmationcode.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeVerificationRequest {

    @NotBlank(message = "{validation.code_verification_request.email.not_blank}")
    @Email(message = "{validation.code_verification_request.email.valid}")
    @Length(min = 8, max = 50, message = "{validation.code_verification_request.email.length}")
    private String email;

    @NotNull(message = "{validation.code_verification_request.code.not_null}")
    private Integer code;
}
