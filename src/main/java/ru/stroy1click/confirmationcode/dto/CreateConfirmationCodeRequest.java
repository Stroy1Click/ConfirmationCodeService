package ru.stroy1click.confirmationcode.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import ru.stroy1click.confirmationcode.entity.Type;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateConfirmationCodeRequest {

    @NotNull(message = "{validation.create_confirmation_code_request.confirmation_code_type.not_null}")
    private Type confirmationCodeType;

    @NotBlank(message = "{validation.create_confirmation_code_request.email.not_blank}")
    @Email(message = "{validation.create_confirmation_code_request.email.valid}")
    @Length(min = 8, max = 50, message = "{validation.create_confirmation_code_request.email.length}")
    private String email;
}
