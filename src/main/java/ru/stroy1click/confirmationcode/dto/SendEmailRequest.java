package ru.stroy1click.confirmationcode.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendEmailRequest {

    private Integer code;

    private UserDto user;
}
