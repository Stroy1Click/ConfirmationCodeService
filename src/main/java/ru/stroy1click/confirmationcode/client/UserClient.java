package ru.stroy1click.confirmationcode.client;

import ru.stroy1click.confirmationcode.dto.UserDto;

public interface UserClient {

    UserDto getByEmail(String email);
}
