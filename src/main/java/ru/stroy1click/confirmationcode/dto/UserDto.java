package ru.stroy1click.confirmationcode.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {

    private Long id;

    @NotBlank(message = "{validation.userdto.first_name.not_blank}")
    @Length(min = 2, max = 30, message = "{validation.userdto.first_name.length}")
    private String firstName;

    @NotBlank(message = "{validation.userdto.last_name.not_blank}")
    @Length(min = 2, max = 30, message = "{validation.userdto.last_name.length}")
    private String lastName;

    @NotBlank(message = "{validation.userdto.email.not_blank}")
    @Email(message = "{validation.userdto.email.valid}")
    @Length(min = 8, max = 50, message = "{validation.userdto.email.length}")
    private String email;

    @NotBlank(message = "{validation.userdto.password.not_blank}")
    @Length(min = 8, max = 60, message = "{validation.userdto.password.length}")
    private String password;

    @NotNull(message = "{validation.userdto.email_confirmed.not_null}")
    private Boolean emailConfirmed;

    @NotNull(message = "{validation.userdto.role.not_null}")
    private Role role;
}
