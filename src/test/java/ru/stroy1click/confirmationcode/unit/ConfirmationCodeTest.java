package ru.stroy1click.confirmationcode.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.context.MessageSource;
import ru.stroy1click.confirmationcode.client.AuthClient;
import ru.stroy1click.confirmationcode.client.EmailClient;
import ru.stroy1click.confirmationcode.client.UserClient;
import ru.stroy1click.confirmationcode.dto.*;
import ru.stroy1click.confirmationcode.entity.ConfirmationCode;
import ru.stroy1click.confirmationcode.entity.Type;
import ru.stroy1click.confirmationcode.exception.NotFoundException;
import ru.stroy1click.confirmationcode.exception.ValidationException;
import ru.stroy1click.confirmationcode.repository.ConfirmationCodeRepository;
import ru.stroy1click.confirmationcode.service.JwtService;
import ru.stroy1click.confirmationcode.service.impl.ConfirmationCodeServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConfirmationCodeTest {

    @Mock
    private ConfirmationCodeRepository confirmationCodeRepository;

    @Mock
    private AuthClient authClient;

    @Mock
    private UserClient userClient;

    @Mock
    private MessageSource messageSource;

    @Mock
    private EmailClient emailClient;

    @Mock
    private JwtService jwtService;


    @InjectMocks
    private ConfirmationCodeServiceImpl confirmationCodeService;

    private UserDto userDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        this.userDto = UserDto.builder()
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
    void create_WhenNoExistingCode_CreatesNewCode() {
        CreateConfirmationCodeRequest request = new CreateConfirmationCodeRequest(Type.EMAIL, "john.doe@example.com");
        ConfirmationCode confirmationCode = new ConfirmationCode(1L, 1234567, LocalDateTime.now().plusHours(24), Type.EMAIL, "john.doe@example.com");
        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(userDto);
        when(this.confirmationCodeRepository.countByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com"))
                .thenReturn(0);
        when(this.confirmationCodeRepository.save(any(ConfirmationCode.class))).thenReturn(confirmationCode);

        this.confirmationCodeService.create(request);

        verify(this.confirmationCodeRepository).save(any(ConfirmationCode.class));
        verify(this.emailClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void create_WhenExistingCode_ThrowsValidationException() {
        CreateConfirmationCodeRequest request = new CreateConfirmationCodeRequest(Type.EMAIL, "john.doe@example.com");
        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(this.userDto);
        when(this.confirmationCodeRepository.countByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com")).thenReturn(1);
        when(this.messageSource.getMessage(eq("error.confirmation_code.already_sent"), any(), any()))
                .thenReturn("Код подтверждения уже был отправлен на вашу почту");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> this.confirmationCodeService.create(request));
        assertEquals("Код подтверждения уже был отправлен на вашу почту", exception.getMessage());

        verify(this.confirmationCodeRepository, never()).save(any());
    }

    @Test
    void create_WhenEmailAlreadyConfirmed_ThrowsValidationException() {
        this.userDto.setEmailConfirmed(true);
        CreateConfirmationCodeRequest request = new CreateConfirmationCodeRequest(Type.EMAIL, "john.doe@example.com");
        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(this.userDto);
        when(this.messageSource.getMessage(eq("error.email.already_confirmed"), any(), any()))
                .thenReturn("Почта уже была подтверждена");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> this.confirmationCodeService.create(request));
        assertEquals("Почта уже была подтверждена", exception.getMessage());

        verify(this.confirmationCodeRepository, never()).save(any());
    }

    @Test
    void recreate_WhenExistingCode_RegeneratesCode() {
        CreateConfirmationCodeRequest request = new CreateConfirmationCodeRequest(Type.EMAIL, "john.doe@example.com");
        ConfirmationCode confirmationCode = new ConfirmationCode(1L, 1234567, LocalDateTime.now().plusHours(24), Type.EMAIL, "john.doe@example.com");
        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(userDto);
        when(this.confirmationCodeRepository.countByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com")).thenReturn(1);
        when(confirmationCodeRepository.save(any(ConfirmationCode.class))).thenReturn(confirmationCode);

        this.confirmationCodeService.recreate(request);

        verify(this.confirmationCodeRepository).deleteByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com");
        verify(this.confirmationCodeRepository).save(any(ConfirmationCode.class));
        verify(this.emailClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void recreate_WhenNoExistingCode_ThrowsValidationException() {
        CreateConfirmationCodeRequest request = new CreateConfirmationCodeRequest(Type.EMAIL, "john.doe@example.com");
        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(this.userDto);
        when(this.confirmationCodeRepository.countByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com")).thenReturn(0);
        when(this.messageSource.getMessage(eq("error.confirmation_code.recreate_failed"), any(), any()))
                .thenReturn("Вы не можете пересоздать код подтверждения, так как письмо не было отправлено на вашу электронную почту");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> this.confirmationCodeService.recreate(request));
        assertEquals("Вы не можете пересоздать код подтверждения, так как письмо не было отправлено на вашу электронную почту",
                exception.getMessage());

        verify(this.confirmationCodeRepository, never()).deleteByTypeAndUserEmail(any(), anyString());
        verify(this.confirmationCodeRepository, never()).save(any());
    }

    @Test
    void confirmEmail_WithValidCode_ConfirmsEmail() {
        CodeVerificationRequest request = new CodeVerificationRequest("john.doe@example.com", 1234567);
        ConfirmationCode confirmationCode = new ConfirmationCode(1L, 1234567,
                LocalDateTime.now().plusHours(1), Type.EMAIL, "john.doe@example.com");

        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(this.userDto);
        when(this.confirmationCodeRepository.findByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com"))
                .thenReturn(Optional.of(confirmationCode));

        this.confirmationCodeService.verifyEmail(request);

        verify(this.userClient).updateEmailConfirmedStatus(new ConfirmEmailRequest("john.doe@example.com"));
        verify(this.confirmationCodeRepository).deleteById(1L);
    }

    @Test
    void confirmEmail_WhenCodeNotFound_ThrowsNotFoundException() {
        CodeVerificationRequest request = new CodeVerificationRequest("john.doe@example.com", 1234567);
        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(this.userDto);
        when(this.confirmationCodeRepository.findByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com"))
                .thenThrow(new NotFoundException("Код подтверждения не найден"));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> this.confirmationCodeService.verifyEmail(request));
        assertEquals("Код подтверждения не найден", exception.getMessage());
    }

    @Test
    void confirmEmail_WithInvalidCode_ThrowsValidationException() {
        CodeVerificationRequest request = new CodeVerificationRequest("john.doe@example.com", 7654321);
        ConfirmationCode confirmationCode = new ConfirmationCode(1L, 1234567,
                LocalDateTime.now().plusHours(1), Type.EMAIL, "john.doe@example.com");

        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(this.userDto);
        when(this.confirmationCodeRepository.findByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com"))
                .thenReturn(Optional.of(confirmationCode));
        when(this.messageSource.getMessage(eq("error.confirmation_code.not_valid"), any(), any()))
                .thenReturn("Код подтверждения не валиден");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> this.confirmationCodeService.verifyEmail(request));
        assertEquals("Код подтверждения не валиден", exception.getMessage());

        verify(this.userClient, never()).updateEmailConfirmedStatus(new ConfirmEmailRequest("john.doe@example.com"));
        verify(this.confirmationCodeRepository, never()).deleteById(anyLong());
    }

    @Test
    void confirmEmail_WithExpiredCode_ThrowsValidationException() {
        CodeVerificationRequest request = new CodeVerificationRequest("john.doe@example.com", 1234567);
        ConfirmationCode confirmationCode = new ConfirmationCode(1L, 1111111,
                LocalDateTime.now().minusHours(1), Type.EMAIL, "john.doe@example.com");

        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(this.userDto);
        when(this.confirmationCodeRepository.findByTypeAndUserEmail(Type.EMAIL, "john.doe@example.com"))
                .thenReturn(Optional.of(confirmationCode));
        when(this.messageSource.getMessage(eq("error.confirmation_code.not_valid"), any(), any()))
                .thenReturn("Код подтверждения не валиден");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> this.confirmationCodeService.verifyEmail(request));
        assertEquals("Код подтверждения не валиден", exception.getMessage());

        verify(this.userClient, never()).updateEmailConfirmedStatus(new ConfirmEmailRequest("john.doe@example.com"));
        verify(this.confirmationCodeRepository, never()).deleteById(anyLong());
    }

    @Test
    void updatePassword_WithValidCodeAndMatchingPasswords_UpdatesPassword() {
        CodeVerificationRequest codeRequest = new CodeVerificationRequest("john.doe@example.com", 1234567);
        UpdatePasswordRequest request = new UpdatePasswordRequest("newPassword123", "newPassword123", codeRequest);
        ConfirmationCode confirmationCode = new ConfirmationCode(1L, 1234567,
                LocalDateTime.now().plusHours(1), Type.PASSWORD, "john.doe@example.com");
        UserServiceUpdatePasswordRequest expectedUpdateRequest = new UserServiceUpdatePasswordRequest(
                "newPassword123", "john.doe@example.com");

        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(this.userDto);
        when(this.confirmationCodeRepository.findByTypeAndUserEmail(Type.PASSWORD, "john.doe@example.com"))
                .thenReturn(Optional.of(confirmationCode));
        when(this.jwtService.generateToken()).thenReturn("generated_token");

        this.confirmationCodeService.updatePassword(request);

        verify(this.userClient).updatePassword(expectedUpdateRequest);
        verify(this.confirmationCodeRepository).deleteByCode(1234567);
        verify(this.authClient).logoutOnAllDevices("john.doe@example.com", "generated_token");
    }

    @Test
    void updatePassword_WhenCodeNotFound_ThrowsNotFoundException() {
        CodeVerificationRequest codeRequest = new CodeVerificationRequest("john.doe@example.com", 1234567);
        UpdatePasswordRequest request = new UpdatePasswordRequest("newPassword123", "newPassword123", codeRequest);

        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(this.userDto);
        when(this.confirmationCodeRepository.findByTypeAndUserEmail(Type.PASSWORD, "john.doe@example.com"))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> this.confirmationCodeService.updatePassword(request));

        verify(this.userClient, never()).updatePassword(any());
        verify(this.confirmationCodeRepository, never()).deleteByCode(anyInt());
        verify(this.authClient, never()).logoutOnAllDevices(anyString(), anyString());
    }

    @Test
    void updatePassword_WithInvalidCode_ThrowsValidationException() {
        CodeVerificationRequest codeRequest = new CodeVerificationRequest("john.doe@example.com", 7654321);
        UpdatePasswordRequest request = new UpdatePasswordRequest("newPassword123", "newPassword123", codeRequest);
        ConfirmationCode confirmationCode = new ConfirmationCode(1L, 1234567,
                LocalDateTime.now().plusHours(1), Type.PASSWORD, "john.doe@example.com");

        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(this.userDto);
        when(this.confirmationCodeRepository.findByTypeAndUserEmail(Type.PASSWORD, "john.doe@example.com"))
                .thenReturn(Optional.of(confirmationCode));
        when(this.messageSource.getMessage(eq("error.confirmation_code.not_valid"), any(), any()))
                .thenReturn("Код подтверждения не валиден");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> this.confirmationCodeService.updatePassword(request));
        assertEquals("Код подтверждения не валиден", exception.getMessage());

        verify(this.userClient, never()).updatePassword(any());
        verify(this.confirmationCodeRepository, never()).deleteByCode(anyInt());
        verify(this.authClient, never()).logoutOnAllDevices(anyString(), anyString());
    }

    @Test
    void updatePassword_WithNonMatchingPasswords_ThrowsValidationException() {
        CodeVerificationRequest codeRequest = new CodeVerificationRequest("john.doe@example.com", 1234567);
        UpdatePasswordRequest request = new UpdatePasswordRequest("newPassword123", "differentPassword", codeRequest);
        ConfirmationCode confirmationCode = new ConfirmationCode(1L, 1234567,
                LocalDateTime.now().plusHours(1), Type.PASSWORD, "john.doe@example.com");

        when(this.userClient.getByEmail("john.doe@example.com")).thenReturn(this.userDto);
        when(this.confirmationCodeRepository.findByTypeAndUserEmail(Type.PASSWORD, "john.doe@example.com"))
                .thenReturn(Optional.of(confirmationCode));
        when(this.messageSource.getMessage(eq("error.password.not_match"), any(), any()))
                .thenReturn("Пароли не совпадают");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> this.confirmationCodeService.updatePassword(request));
        assertEquals("Пароли не совпадают", exception.getMessage());

        verify(this.userClient, never()).updatePassword(any());
        verify(this.confirmationCodeRepository, never()).deleteByCode(anyInt());
        verify(this.authClient, never()).logoutOnAllDevices(anyString(), anyString());
    }
}
