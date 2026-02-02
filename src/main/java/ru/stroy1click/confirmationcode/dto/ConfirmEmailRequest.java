package ru.stroy1click.confirmationcode.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmEmailRequest {

    @Email(message = "{validation.confirm_email_request.email.valid}")
    @NotBlank(message = "{validation.confirm_email_request.email.not_blank}")
    private String email;
}
