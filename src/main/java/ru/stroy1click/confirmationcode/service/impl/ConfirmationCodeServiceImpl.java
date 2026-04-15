package ru.stroy1click.confirmationcode.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.stroy1click.common.command.ConfirmEmailCommand;
import ru.stroy1click.common.command.LogoutOnAllDevicesCommand;
import ru.stroy1click.common.command.SendEmailCommand;
import ru.stroy1click.common.command.UpdatePasswordCommand;
import ru.stroy1click.common.util.ExceptionUtils;
import ru.stroy1click.confirmationcode.client.UserClient;
import ru.stroy1click.confirmationcode.dto.CodeVerificationRequest;
import ru.stroy1click.confirmationcode.dto.CreateConfirmationCodeRequest;
import ru.stroy1click.confirmationcode.dto.UpdatePasswordRequest;
import ru.stroy1click.confirmationcode.dto.UserDto;
import ru.stroy1click.confirmationcode.entity.ConfirmationCode;
import ru.stroy1click.confirmationcode.entity.Type;
import ru.stroy1click.confirmationcode.repository.ConfirmationCodeRepository;
import ru.stroy1click.confirmationcode.service.ConfirmationCodeService;
import ru.stroy1click.outbox.service.OutboxEventService;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Random;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ConfirmationCodeServiceImpl implements ConfirmationCodeService {

    private final ConfirmationCodeRepository confirmationCodeRepository;

    private final UserClient userClient;

    private final PasswordEncoder passwordEncoder;

    private final static Integer EXPIRATION = 24;

    private final OutboxEventService outboxEventService;

    private final static String SEND_EMAIL_TOPIC = "send-email-commands";

    private final static String CONFIRM_EMAIL_TOPIC = "confirm-email-commands";

    private final static String LOGOUT_ON_ALL_DEVICES_TOPIC = "logout-on-all-devices-commands";

    private final static String UPDATE_PASSWORD_TOPIC = "update-password-commands";

    /**
     * Метод создает новый код подтверждения для пользователя.
     * Код подтверждения уникален для каждого пользователя и имеет тип.
     * Метод проверяет, существует ли у пользователя уже код подтверждения с таким же типом.
     * Если у пользователя уже есть код подтверждения с таким типом, метод выбрасывает ValidationException.
     * Метод сохраняет код подтверждения в бд и отправляет в email-service запрос на отправку электронного письма.
     */
    @Override
    @Transactional
    public void create(CreateConfirmationCodeRequest codeRequest) {
        UserDto user = this.userClient.getByEmail(codeRequest.getEmail());

        Integer countOfConfirmationCode = countCodesByTypeAndUser(user.getEmail(), codeRequest);

        if(countOfConfirmationCode >= 1){ //The capacities allow you to send only 1 email
            throw ExceptionUtils.validationException("error.confirmation_code.already_sent", null);
        }

        checkTheEmailConfirmation(user, codeRequest);

        ConfirmationCode confirmationCode = this.confirmationCodeRepository.save(new ConfirmationCode(
                null, new Random().nextInt(1_000_000, 9_999_999),LocalDateTime.now().plusHours(EXPIRATION),
                codeRequest.getConfirmationCodeType(), user.getEmail()
        ));

        this.outboxEventService.save(SEND_EMAIL_TOPIC,
                SendEmailCommand.builder()
                .code(confirmationCode.getCode())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build()
        );
    }

    /**
    * Метод повторно создает код подтверждения для пользователя.
    * Код подтверждения уникален для каждого пользователя и имеет тип.
    * Метод проверяет, существует ли у пользователя уже код подтверждения с таким же типом.
    * Если у пользователя уже есть код подтверждения с таким типом, метод удаляет старый код подтверждения и создает новый.
    * Если у пользователя нет кода подтверждения с таким типом, метод выбрасывает ValidationException.
    * Метод сохраняет код подтверждения в бд и отправляет в email-service запрос на отправку электронного письма.
    */
    @Override
    public void recreate(CreateConfirmationCodeRequest codeRequest) {
        UserDto user = this.userClient.getByEmail(codeRequest.getEmail());

        Integer countOfConfirmationCode = countCodesByTypeAndUser(user.getEmail(), codeRequest);

        if(countOfConfirmationCode  == 0){ //не можем пересоздать код подтверждения, если его даже не было никогда
            throw ExceptionUtils.validationException("error.confirmation_code.recreate_failed", null);
        }

        this.confirmationCodeRepository.deleteByTypeAndUserEmail(codeRequest.getConfirmationCodeType(), user.getEmail());

        checkTheEmailConfirmation(user, codeRequest);

        ConfirmationCode confirmationCode = this.confirmationCodeRepository.save(new ConfirmationCode(
                null, new Random().nextInt(1_000_000, 9_999_999), LocalDateTime.now().plusHours(EXPIRATION),
                codeRequest.getConfirmationCodeType(), user.getEmail()
        ));

        this.outboxEventService.save(SEND_EMAIL_TOPIC,
                SendEmailCommand.builder()
                        .code(confirmationCode.getCode())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .build()
        );
    }

    /**
    * Метод подтверждает электронную почту пользователя.
    * Метод проверяет, действителен ли запрос на подтверждение кода.
    * Если запрос на подтверждение кода действителен, метод обновляет статус подтверждения электронной почты пользователя и удаляет код подтверждения.
    * Если запрос на подтверждение кода недействителен, метод выбрасывает ValidationException.
    */
    @Override
    public void verifyEmail(CodeVerificationRequest codeRequest) {
        ConfirmationCode confirmationCode = this.confirmationCodeRepository.findByTypeAndUserEmail(Type.EMAIL, codeRequest.getEmail())
                .orElseThrow(() -> ExceptionUtils.notFound("error.confirmation_code.not_found", null));

        if(Objects.equals(confirmationCode.getCode(), codeRequest.getCode()) && LocalDateTime.now().isBefore(confirmationCode.getExpirationDate())){
            this.confirmationCodeRepository.deleteById(confirmationCode.getId());

            this.outboxEventService.save(CONFIRM_EMAIL_TOPIC,
                    new ConfirmEmailCommand(codeRequest.getEmail()));
        } else {
            throw ExceptionUtils.validationException("error.confirmation_code.not_valid", null);
        }
    }

     /**
    * Метод обновляет пароль пользователя.
    * Метод проверяет, действителен ли запрос на подтверждение кода.
    * Если запрос на подтверждение кода действителен, метод обновляет пароль пользователя и удаляет код подтверждения.
    * Если запрос на подтверждение кода недействителен, метод выбрасывает ValidationException.
    * Метод также удаляет все refresh-токены пользователя.
    * @param passwordRequest запрос с новым паролем и кодом подтверждения.
    */
    @Override
    public void updatePassword(UpdatePasswordRequest passwordRequest) {
        ConfirmationCode confirmationCode =  this.confirmationCodeRepository.findByTypeAndUserEmail(Type.PASSWORD,
                        passwordRequest.getCodeVerificationRequest().getEmail())
                .orElseThrow(() -> ExceptionUtils.notFound("error.confirmation_code.not_found", null));

        if(!Objects.equals(confirmationCode.getCode(), passwordRequest.getCodeVerificationRequest().getCode()) ||
                LocalDateTime.now().isAfter(confirmationCode.getExpirationDate())){
            throw ExceptionUtils.validationException("error.confirmation_code.not_valid", null);
        }
        if(!Objects.equals(passwordRequest.getNewPassword(), passwordRequest.getConfirmPassword())){
            throw ExceptionUtils.validationException("error.password.not_match", null);
        }

        this.confirmationCodeRepository.deleteByCode(confirmationCode.getCode());

        this.outboxEventService.save(LOGOUT_ON_ALL_DEVICES_TOPIC,
                new LogoutOnAllDevicesCommand(passwordRequest.getCodeVerificationRequest().getEmail()));
        String encodedNewPassword = this.passwordEncoder.encode(passwordRequest.getNewPassword());
        this.outboxEventService.save(UPDATE_PASSWORD_TOPIC,
                new UpdatePasswordCommand(encodedNewPassword, passwordRequest.getCodeVerificationRequest().getEmail()));
    }

      /**
    * Метод проверяет, подтвердил ли пользователь свою электронную почту.
    * Если пользователь уже подтвердил свою электронную почту и тип кода подтверждения - EMAIL,
    * метод выбрасывает ValidationException.
    * @param user пользователь для проверки.
    * @param codeRequest запрос с типом кода подтверждения.
    */
    private void checkTheEmailConfirmation(UserDto user, CreateConfirmationCodeRequest codeRequest) {
        if(user.getIsEmailConfirmed() && codeRequest.getConfirmationCodeType() == Type.EMAIL){
            throw ExceptionUtils.validationException("error.email.already_confirmed", null);
        }
    }

     /**
    * Метод подсчитывает количество кодов подтверждения пользователя по типу.
    * Метод возвращает количество кодов подтверждения.
    * @param email пользователь, для которого подсчитываются коды подтверждения.
    * @param codeRequest запрос с типом кода подтверждения.
    * @return количество кодов подтверждения.
    */
    private Integer countCodesByTypeAndUser(String email, CreateConfirmationCodeRequest codeRequest) {
        Integer count = 0;
        switch(codeRequest.getConfirmationCodeType()){
            case EMAIL -> count += this.confirmationCodeRepository.countByTypeAndUserEmail(Type.EMAIL, email);
            case PASSWORD -> count += this.confirmationCodeRepository.countByTypeAndUserEmail(Type.PASSWORD, email);
            default -> throw ExceptionUtils.validationException("error.confirmation_code.not_valid", null);
        }
        return count;
    }
}