package ru.stroy1click.confirmationcode.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePasswordRequest {

    @NotBlank(message = "{validation.update_password_request.new_password.not_blank}")
    @Length(min = 8, max = 60, message = "{validation.update_password_request.new_password.length}")
    private String newPassword;

    @NotBlank(message = "{validation.update_password_request.confirm_password.not_blank}")
    @Length(min = 8, max = 60, message = "{validation.update_password_request.confirm_password.length}")
    private String confirmPassword;

    @NotNull(message = "{validation.update_password_request.code_verification_request.not_null}")
    private CodeVerificationRequest codeVerificationRequest;

}
