package ru.stroy1click.confirmationcode.service;

import ru.stroy1click.confirmationcode.dto.CodeVerificationRequest;
import ru.stroy1click.confirmationcode.dto.CreateConfirmationCodeRequest;
import ru.stroy1click.confirmationcode.dto.UpdatePasswordRequest;

public interface ConfirmationCodeService {

    void create(CreateConfirmationCodeRequest codeRequest);

    void recreate(CreateConfirmationCodeRequest codeRequest);

    void verifyEmail(CodeVerificationRequest codeRequest);

    void updatePassword(UpdatePasswordRequest passwordRequest);
}
