package ru.stroy1click.confirmationcode.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.stroy1click.confirmationcode.client.AuthClient;
import ru.stroy1click.confirmationcode.client.EmailClient;
import ru.stroy1click.confirmationcode.client.UserClient;
import ru.stroy1click.confirmationcode.dto.*;
import ru.stroy1click.confirmationcode.entity.ConfirmationCode;
import ru.stroy1click.confirmationcode.entity.Type;
import ru.stroy1click.confirmationcode.exception.NotFoundException;
import ru.stroy1click.confirmationcode.exception.ValidationException;
import ru.stroy1click.confirmationcode.repository.ConfirmationCodeRepository;
import ru.stroy1click.confirmationcode.service.ConfirmationCodeService;
import ru.stroy1click.confirmationcode.service.JwtService;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ConfirmationCodeServiceImpl implements ConfirmationCodeService {

    private final ConfirmationCodeRepository confirmationCodeRepository;

    private final AuthClient authClient;

    private final UserClient userClient;

    private final static Integer EXPIRATION = 24;

    private final Random random = new Random();

    private final MessageSource messageSource;

    private final EmailClient emailClient;

    private final JwtService jwtService;

    /**
     * Метод создает новый код подтверждения для пользователя.
     * Код подтверждения уникален для каждого пользователя и имеет тип.
     * Метод проверяет, существует ли у пользователя уже код подтверждения с таким же типом.
     * Если у пользователя уже есть код подтверждения с таким типом, метод выбрасывает ValidationException.
     * Метод сохраняет код подтверждения в бд и отправляет в email-service запрос на отправку электронного письма.
     */
    @Override
    public void create(CreateConfirmationCodeRequest codeRequest) {
        UserDto user = this.userClient.getByEmail(codeRequest.getEmail());

        Integer countOfConfirmationCode = countCodesByTypeAndUser(user.getEmail(), codeRequest);

        if(countOfConfirmationCode >= 1){ //The capacities allow you to send only 1 email
            throw new ValidationException(
                    this.messageSource.getMessage(
                            "error.confirmation_code.already_sent",
                            null,
                            Locale.getDefault()
                    )
            );
        }

        checkTheEmailConfirmation(user, codeRequest);

        ConfirmationCode confirmationCode = this.confirmationCodeRepository.save(new ConfirmationCode(
                null, this.random.nextInt(1_000_000, 9_999_999),LocalDateTime.now().plusHours(EXPIRATION),
                codeRequest.getConfirmationCodeType(), user.getEmail()
        ));

        sendEmail(confirmationCode.getCode(), user);
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
            throw new ValidationException(
                    this.messageSource.getMessage(
                            "error.confirmation_code.recreate_failed",
                            null,
                            Locale.getDefault()
                    )
            );
        }

        this.confirmationCodeRepository.deleteByTypeAndUserEmail(codeRequest.getConfirmationCodeType(), user.getEmail());

        checkTheEmailConfirmation(user, codeRequest);

        ConfirmationCode confirmationCode = this.confirmationCodeRepository.save(new ConfirmationCode(
                null, this.random.nextInt(1_000_000, 9_999_999), LocalDateTime.now().plusHours(EXPIRATION),
                codeRequest.getConfirmationCodeType(), user.getEmail()
        ));

        sendEmail(confirmationCode.getCode(), user);
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
                .orElseThrow(() -> new NotFoundException(
                        this.messageSource.getMessage(
                                "error.confirmation_code.not_found",
                                null,
                                Locale.getDefault()
                        )
                ));

        if(Objects.equals(confirmationCode.getCode(), codeRequest.getCode()) && LocalDateTime.now().isBefore(confirmationCode.getExpirationDate())){
            this.userClient.updateEmailConfirmedStatus(new ConfirmEmailRequest(codeRequest.getEmail()));
            this.confirmationCodeRepository.deleteById(confirmationCode.getId());
        } else {
            throw new ValidationException(
                    this.messageSource.getMessage(
                            "error.confirmation_code.not_valid",
                            null,
                            Locale.getDefault()
                    )
            );
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
                .orElseThrow(() -> new NotFoundException(this.messageSource.getMessage(
                        "error.confirmation_code.not_found",
                        null,
                        Locale.getDefault()
                )));

        if(!Objects.equals(confirmationCode.getCode(), passwordRequest.getCodeVerificationRequest().getCode()) ||
                LocalDateTime.now().isAfter(confirmationCode.getExpirationDate())){
            throw new ValidationException(
                    this.messageSource.getMessage(
                            "error.confirmation_code.not_valid",
                            null,
                            Locale.getDefault()
                    )
            );
        }
        if(!Objects.equals(passwordRequest.getNewPassword(), passwordRequest.getConfirmPassword())){
            throw new ValidationException(
                    this.messageSource.getMessage(
                            "error.password.not_match",
                            null,
                            Locale.getDefault()
                    )
            );
        }

        this.confirmationCodeRepository.deleteByCode(confirmationCode.getCode());
        this.authClient.logoutOnAllDevices(passwordRequest.getCodeVerificationRequest().getEmail(),
                this.jwtService.generateToken());
        this.userClient.updatePassword(new UserServiceUpdatePasswordRequest(passwordRequest.getNewPassword(),
                passwordRequest.getCodeVerificationRequest().getEmail()));
    }

      /**
    * Метод проверяет, подтвердил ли пользователь свою электронную почту.
    * Если пользователь уже подтвердил свою электронную почту и тип кода подтверждения - EMAIL,
    * метод выбрасывает ValidationException.
    * @param user пользователь для проверки.
    * @param codeRequest запрос с типом кода подтверждения.
    */
    private void checkTheEmailConfirmation(UserDto user, CreateConfirmationCodeRequest codeRequest){
        if(user.getEmailConfirmed() && codeRequest.getConfirmationCodeType() == Type.EMAIL){
            throw new ValidationException(
                    this.messageSource.getMessage(
                            "error.email.already_confirmed",
                            null,
                            Locale.getDefault()
                    )
            );
        }
    }

     /**
    * Метод подсчитывает количество кодов подтверждения пользователя по типу.
    * Метод возвращает количество кодов подтверждения.
    * @param email пользователь, для которого подсчитываются коды подтверждения.
    * @param codeRequest запрос с типом кода подтверждения.
    * @return количество кодов подтверждения.
    */
    private Integer countCodesByTypeAndUser(String email, CreateConfirmationCodeRequest codeRequest){
        Integer count = 0;
        switch(codeRequest.getConfirmationCodeType()){
            case EMAIL -> count += this.confirmationCodeRepository.countByTypeAndUserEmail(Type.EMAIL, email);
            case PASSWORD -> count += this.confirmationCodeRepository.countByTypeAndUserEmail(Type.PASSWORD, email);
            default -> throw new ValidationException(
                    this.messageSource.getMessage(
                            "error.confirmation_code.not_valid",
                            null,
                            Locale.getDefault()
                    )
            );
        }
        return count;
    }

    private void sendEmail(Integer code, UserDto user){
        SendEmailRequest sendEmailRequest = new SendEmailRequest(code, user);
        this.emailClient.sendEmail(sendEmailRequest);
    }
}