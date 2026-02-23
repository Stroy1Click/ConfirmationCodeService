package ru.stroy1click.confirmationcode.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import ru.stroy1click.common.command.ConfirmEmailCommand;
import ru.stroy1click.common.command.LogoutOnAllDevicesCommand;
import ru.stroy1click.common.command.SendEmailCommand;
import ru.stroy1click.common.command.UpdatePasswordCommand;
import ru.stroy1click.common.exception.NotFoundException;
import ru.stroy1click.common.exception.ValidationException;
import ru.stroy1click.confirmationcode.client.UserClient;
import ru.stroy1click.confirmationcode.dto.*;
import ru.stroy1click.confirmationcode.entity.ConfirmationCode;
import ru.stroy1click.confirmationcode.entity.Type;
import ru.stroy1click.confirmationcode.repository.ConfirmationCodeRepository;
import ru.stroy1click.confirmationcode.service.impl.ConfirmationCodeServiceImpl;
import ru.stroy1click.outbox.service.OutboxEventService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmationCodeServiceTest {

    @Mock
    private ConfirmationCodeRepository confirmationCodeRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private MessageSource messageSource;

    @Mock
    private OutboxEventService outboxEventService;

    @InjectMocks
    private ConfirmationCodeServiceImpl confirmationCodeService;

    private final static String SEND_EMAIL_TOPIC = "send-email-commands";

    private final static String CONFIRM_EMAIL_TOPIC = "confirm-email-commands";

    private final static String LOGOUT_ON_ALL_DEVICES_TOPIC = "logout-on-all-devices-commands";

    private final static String UPDATE_PASSWORD_TOPIC = "update-password-commands";

    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userDto = UserDto.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("encoded_password")
                .role(Role.ROLE_USER)
                .emailConfirmed(false)
                .build();
    }

    @Test
    void create_WhenNoExistingCode_CreatesNewCodeAndSaveOutboxEvent() {
        //Arrange
        CreateConfirmationCodeRequest request = new CreateConfirmationCodeRequest(Type.EMAIL, "john.doe@example.com");
        ConfirmationCode confirmationCode = new ConfirmationCode(1L, 1234567, LocalDateTime.now().plusHours(24), Type.EMAIL, "john.doe@example.com");
        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(userDto);
        when(this.confirmationCodeRepository.countByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com"))
                .thenReturn(0);
        when(this.confirmationCodeRepository.save(any(ConfirmationCode.class))).thenReturn(confirmationCode);
        doNothing().when(this.outboxEventService).save(eq(SEND_EMAIL_TOPIC), any(SendEmailCommand.class));

        //Act
        this.confirmationCodeService.create(request);

        //Assert
        verify(this.confirmationCodeRepository).save(any(ConfirmationCode.class));
        verify(this.outboxEventService).save(eq(SEND_EMAIL_TOPIC), any(SendEmailCommand.class));
    }

    @Test
    void create_WhenCodeIsAlreadyExists_ShouldThrowValidationException() {
        //Arrange
        CreateConfirmationCodeRequest request = new CreateConfirmationCodeRequest(Type.EMAIL, "john.doe@example.com");
        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(this.userDto);
        when(this.confirmationCodeRepository.countByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com")).thenReturn(1);
        when(this.messageSource.getMessage(eq("error.confirmation_code.already_sent"), any(), any()))
                .thenReturn("Код подтверждения уже был отправлен на вашу почту");

        //Act
        ValidationException exception = assertThrows(ValidationException.class,
                () -> this.confirmationCodeService.create(request));

        //Assert
        assertEquals("Код подтверждения уже был отправлен на вашу почту", exception.getMessage());
        verify(this.confirmationCodeRepository, never()).save(any());
    }

    @Test
    void create_WhenEmailAlreadyConfirmed_ShouldThrowValidationException() {
        //Arrange
        this.userDto.setEmailConfirmed(true);
        CreateConfirmationCodeRequest request = new CreateConfirmationCodeRequest(Type.EMAIL, "john.doe@example.com");
        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(this.userDto);
        when(this.messageSource.getMessage(eq("error.email.already_confirmed"), any(), any()))
                .thenReturn("Почта уже была подтверждена");

        //Act
        ValidationException exception = assertThrows(ValidationException.class,
                () -> this.confirmationCodeService.create(request));

        //Assert
        assertEquals("Почта уже была подтверждена", exception.getMessage());
        verify(this.confirmationCodeRepository, never()).save(any());
    }

    @Test
    void recreate_WhenExistingCode_RegeneratesCodeAndSaveOutboxEvent() {
        //Arrange
        CreateConfirmationCodeRequest request = new CreateConfirmationCodeRequest(Type.EMAIL, "john.doe@example.com");
        ConfirmationCode confirmationCode = new ConfirmationCode(1L, 1234567, LocalDateTime.now().plusHours(24), Type.EMAIL, "john.doe@example.com");
        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(userDto);
        when(this.confirmationCodeRepository.countByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com")).thenReturn(1);
        when(confirmationCodeRepository.save(any(ConfirmationCode.class))).thenReturn(confirmationCode);
        doNothing().when(this.outboxEventService).save(eq(SEND_EMAIL_TOPIC), any(SendEmailCommand.class));

        //Act
        this.confirmationCodeService.recreate(request);

        //Assert
        verify(this.confirmationCodeRepository).deleteByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com");
        verify(this.confirmationCodeRepository).save(any(ConfirmationCode.class));
        verify(this.outboxEventService).save(eq(SEND_EMAIL_TOPIC), any(SendEmailCommand.class));
    }

    @Test
    void recreate_WhenNoExistingCode_ShouldThrowValidationException() {
        //Arrange
        CreateConfirmationCodeRequest request = new CreateConfirmationCodeRequest(Type.EMAIL, "john.doe@example.com");
        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(this.userDto);
        when(this.confirmationCodeRepository.countByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com")).thenReturn(0);
        when(this.messageSource.getMessage(eq("error.confirmation_code.recreate_failed"), any(), any()))
                .thenReturn("Вы не можете пересоздать код подтверждения, так как письмо не было отправлено на вашу электронную почту");

        //Act
        ValidationException exception = assertThrows(ValidationException.class,
                () -> this.confirmationCodeService.recreate(request));

        //Assert
        assertEquals("Вы не можете пересоздать код подтверждения, так как письмо не было отправлено на вашу электронную почту",
                exception.getMessage());
        verify(this.confirmationCodeRepository, never()).deleteByTypeAndUserEmail(any(), anyString());
        verify(this.confirmationCodeRepository, never()).save(any());
    }

    @Test
    void verifyEmail_WithValidCodeProvided_ConfirmsEmailAndSaveOutboxEvent() {
        //Arrange
        CodeVerificationRequest request = new CodeVerificationRequest("john.doe@example.com", 1234567);
        ConfirmationCode confirmationCode = new ConfirmationCode(1L, 1234567,
                LocalDateTime.now().plusHours(1), Type.EMAIL, "john.doe@example.com");
        when(this.confirmationCodeRepository.findByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com"))
                .thenReturn(Optional.of(confirmationCode));
        doNothing().when(this.outboxEventService).save(eq(CONFIRM_EMAIL_TOPIC), any(ConfirmEmailCommand.class));

        //Act
        this.confirmationCodeService.verifyEmail(request);

        //Assert
        verify(this.confirmationCodeRepository).deleteById(1L);
        verify(this.outboxEventService).save(eq(CONFIRM_EMAIL_TOPIC), any(ConfirmEmailCommand.class));
    }

    @Test
    void verifyEmail_WhenCodeNotFound_ShouldThrowNotFoundException() {
        //Arrange
        CodeVerificationRequest request = new CodeVerificationRequest("john.doe@example.com", 1234567);
        when(this.confirmationCodeRepository.findByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com"))
                .thenThrow(new NotFoundException("Код подтверждения не найден"));

        //Act
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> this.confirmationCodeService.verifyEmail(request));

        //Assert
        assertEquals("Код подтверждения не найден", exception.getMessage());
    }

    @Test
    void verifyEmail_WithInvalidCode_ShouldThrowValidationException() {
        //Arrange
        CodeVerificationRequest request = new CodeVerificationRequest("john.doe@example.com", 7654321);
        ConfirmationCode confirmationCode = new ConfirmationCode(1L, 1234567,
                LocalDateTime.now().plusHours(1), Type.EMAIL, "john.doe@example.com");

        when(this.confirmationCodeRepository.findByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com"))
                .thenReturn(Optional.of(confirmationCode));
        when(this.messageSource.getMessage(eq("error.confirmation_code.not_valid"), any(), any()))
                .thenReturn("Код подтверждения не валиден");

        //Act
        ValidationException exception = assertThrows(ValidationException.class,
                () -> this.confirmationCodeService.verifyEmail(request));

        //Assert
        assertEquals("Код подтверждения не валиден", exception.getMessage());
        verify(this.outboxEventService, never()).save(eq(CONFIRM_EMAIL_TOPIC), any(ConfirmEmailCommand.class));
        verify(this.confirmationCodeRepository, never()).deleteById(anyLong());
    }

    @Test
    void verifyEmail_WithExpiredCode_ShouldThrowValidationException() {
        //Arrange
        CodeVerificationRequest request = new CodeVerificationRequest("john.doe@example.com", 1234567);
        ConfirmationCode confirmationCode = new ConfirmationCode(1L, 1111111,
                LocalDateTime.now().minusHours(1), Type.EMAIL, "john.doe@example.com");

        when(this.confirmationCodeRepository.findByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com"))
                .thenReturn(Optional.of(confirmationCode));
        when(this.messageSource.getMessage(eq("error.confirmation_code.not_valid"), any(), any()))
                .thenReturn("Код подтверждения не валиден");

        //Act
        ValidationException exception = assertThrows(ValidationException.class,
                () -> this.confirmationCodeService.verifyEmail(request));

        //Assert
        assertEquals("Код подтверждения не валиден", exception.getMessage());
        verify(this.outboxEventService, never()).save(eq(CONFIRM_EMAIL_TOPIC), any(ConfirmEmailCommand.class));
        verify(this.confirmationCodeRepository, never()).deleteById(anyLong());
    }

    @Test
    void updatePassword_WithValidCodeAndMatchingPasswords_UpdatesPasswordAndSaveOutboxEvents() {
        //Arrange
        CodeVerificationRequest codeRequest = new CodeVerificationRequest("john.doe@example.com", 1234567);
        UpdatePasswordRequest request = new UpdatePasswordRequest("newPassword123", "newPassword123", codeRequest);
        ConfirmationCode confirmationCode = new ConfirmationCode(1L, 1234567,
                LocalDateTime.now().plusHours(1), Type.PASSWORD, "john.doe@example.com");

        when(this.confirmationCodeRepository.findByTypeAndUserEmail(Type.PASSWORD, "john.doe@example.com"))
                .thenReturn(Optional.of(confirmationCode));
        doNothing().when(this.outboxEventService).save(eq(UPDATE_PASSWORD_TOPIC), any(UpdatePasswordCommand.class));
        doNothing().when(this.outboxEventService).save(eq(LOGOUT_ON_ALL_DEVICES_TOPIC), any(LogoutOnAllDevicesCommand.class));

        //Act
        this.confirmationCodeService.updatePassword(request);

        //Assert
        verify(this.confirmationCodeRepository).deleteByCode(1234567);
        verify(this.outboxEventService).save(eq(UPDATE_PASSWORD_TOPIC), any(UpdatePasswordCommand.class));
        verify(this.outboxEventService).save(eq(LOGOUT_ON_ALL_DEVICES_TOPIC), any(LogoutOnAllDevicesCommand.class));
    }

    @Test
    void updatePassword_WhenCodeNotFound_ShouldThrowNotFoundException() {
        //Arrange
        CodeVerificationRequest codeRequest = new CodeVerificationRequest("john.doe@example.com", 1234567);
        UpdatePasswordRequest request = new UpdatePasswordRequest("newPassword123", "newPassword123", codeRequest);
        when(this.confirmationCodeRepository.findByTypeAndUserEmail(Type.PASSWORD, "john.doe@example.com"))
                .thenReturn(Optional.empty());

        //Act
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> this.confirmationCodeService.updatePassword(request));

        //Assert
        verify(this.confirmationCodeRepository, never()).deleteByCode(anyInt());
        verify(this.outboxEventService, never()).save(eq(UPDATE_PASSWORD_TOPIC), any(UpdatePasswordRequest.class));
    }

    @Test
    void updatePassword_WithInvalidCode_ShouldThrowValidationException() {
        //Arrange
        CodeVerificationRequest codeRequest = new CodeVerificationRequest("john.doe@example.com", 7654321);
        UpdatePasswordRequest request = new UpdatePasswordRequest("newPassword123", "newPassword123", codeRequest);
        ConfirmationCode confirmationCode = new ConfirmationCode(1L, 1234567,
                LocalDateTime.now().plusHours(1), Type.PASSWORD, "john.doe@example.com");

        when(this.confirmationCodeRepository.findByTypeAndUserEmail(Type.PASSWORD, "john.doe@example.com"))
                .thenReturn(Optional.of(confirmationCode));
        when(this.messageSource.getMessage(eq("error.confirmation_code.not_valid"), any(), any()))
                .thenReturn("Код подтверждения не валиден");

        //Act
        ValidationException exception = assertThrows(ValidationException.class,
                () -> this.confirmationCodeService.updatePassword(request));

        //Assert
        assertEquals("Код подтверждения не валиден", exception.getMessage());
        verify(this.confirmationCodeRepository, never()).deleteByCode(anyInt());
        verify(this.outboxEventService, never()).save(eq(UPDATE_PASSWORD_TOPIC), any(UpdatePasswordRequest.class));
    }

    @Test
    void updatePassword_WithNonMatchingPasswords_ShouldThrowValidationException() {
        //Arrange
        CodeVerificationRequest codeRequest = new CodeVerificationRequest("john.doe@example.com", 1234567);
        UpdatePasswordRequest request = new UpdatePasswordRequest("newPassword123", "differentPassword", codeRequest);
        ConfirmationCode confirmationCode = new ConfirmationCode(1L, 1234567,
                LocalDateTime.now().plusHours(1), Type.PASSWORD, "john.doe@example.com");

        when(this.confirmationCodeRepository.findByTypeAndUserEmail(Type.PASSWORD, "john.doe@example.com"))
                .thenReturn(Optional.of(confirmationCode));
        when(this.messageSource.getMessage(eq("error.password.not_match"), any(), any()))
                .thenReturn("Пароли не совпадают");

        //Act
        ValidationException exception = assertThrows(ValidationException.class,
                () -> this.confirmationCodeService.updatePassword(request));

        //Assert
        assertEquals("Пароли не совпадают", exception.getMessage());
        verify(this.confirmationCodeRepository, never()).deleteByCode(anyInt());
        verify(this.outboxEventService, never()).save(eq(UPDATE_PASSWORD_TOPIC), any(UpdatePasswordRequest.class));
    }
}
