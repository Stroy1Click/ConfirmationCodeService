package ru.stroy1click.confirmationcode.client;

import ru.stroy1click.confirmationcode.dto.UserDto;
import ru.stroy1click.confirmationcode.dto.ConfirmEmailRequest;
import ru.stroy1click.confirmationcode.dto.UserServiceUpdatePasswordRequest;

public interface UserClient {

    void updateEmailConfirmedStatus(ConfirmEmailRequest confirmEmailRequest);

    void updatePassword(UserServiceUpdatePasswordRequest request);

    UserDto getByEmail(String email);
}
