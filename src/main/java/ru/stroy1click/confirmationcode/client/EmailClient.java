package ru.stroy1click.confirmationcode.client;

import ru.stroy1click.confirmationcode.dto.SendEmailRequest;

public interface EmailClient {

    void sendEmail(SendEmailRequest sendEmailRequest);
}
