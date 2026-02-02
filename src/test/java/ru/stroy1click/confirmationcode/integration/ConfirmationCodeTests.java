package ru.stroy1click.confirmationcode.integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.stroy1click.confirmationcode.client.AuthClient;
import ru.stroy1click.confirmationcode.client.EmailClient;
import ru.stroy1click.confirmationcode.client.UserClient;
import ru.stroy1click.confirmationcode.dto.*;
import ru.stroy1click.confirmationcode.entity.Type;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Import({TestcontainersConfiguration.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfirmationCodeTests {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @MockitoBean
    private EmailClient emailClient;

    @MockitoBean
    private AuthClient authClient;

    @MockitoBean
    private UserClient userClient;

    @Test
    public void create_ValidUser_CreatesConfirmationCode() {
        UserDto userDto = UserDto.builder()
                .id(1L)
                .firstName("Ryan")
                .lastName("Thompson")
                .email("rayan_thompson@gmail.com")
                .password("$2a$12$5AvRdljjFvz1gJtVioGOJ./tAV8KHjln/fvKjrRXMAUxxqjYN4Vpi")
                .role(Role.ROLE_USER)
                .emailConfirmed(false)
                .build();
        HttpEntity<CreateConfirmationCodeRequest> httpEntity = new HttpEntity<>(new CreateConfirmationCodeRequest(Type.EMAIL,
                "rayan_thompson@gmail.com"));

        when(this.userClient.getByEmail("rayan_thompson@gmail.com")).thenReturn(userDto);
        doNothing().when(this.emailClient).sendEmail(any(SendEmailRequest.class));

        ResponseEntity<String> responseEntity = this.testRestTemplate.exchange(
                "/api/v1/confirmation-codes",
                HttpMethod.POST,
                httpEntity,
                String.class
        );

        System.out.println(responseEntity);
        Assertions.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        Assertions.assertEquals("Код подтверждения успешно отправлен на электронную почту", responseEntity.getBody());
    }

    @Test
    public void confirmEmail_ValidCode_ConfirmsEmail() {
        UserDto userDto = UserDto.builder()
                .id(2L)
                .firstName("Jeff")
                .lastName("Bezos")
                .email("jeffbezos@gmail.com")
                .password("$2a$12$5AvRdljjFvz1gJtVioGOJ./tAV8KHjln/fvKjrRXMAUxxqjYN4Vpi")
                .role(Role.ROLE_USER)
                .emailConfirmed(false)
                .build();
        HttpEntity<CodeVerificationRequest> httpEntity = new HttpEntity<>(new CodeVerificationRequest("jeffbezos@gmail.com", 1_234_567));

        when(this.userClient.getByEmail("jeffbezos@gmail.com")).thenReturn(userDto);

        ResponseEntity<String> responseEntity = this.testRestTemplate.exchange(
                "/api/v1/confirmation-codes/email/verify",
                HttpMethod.POST,
                httpEntity,
                String.class
        );

        System.out.println(responseEntity);

        Assertions.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        Assertions.assertEquals("Электронная почта подтверждена", responseEntity.getBody());
    }

    @Test
    public void recreate_ExistingCode_RegeneratesConfirmationCode() {
        UserDto userDto = UserDto.builder()
                .id(3L)
                .firstName("Donald")
                .lastName("Trump")
                .email("donaldtrump@gmail.com")
                .password("$2a$12$5AvRdljjFvz1gJtVioGOJ./tAV8KHjln/fvKjrRXMAUxxqjYN4Vpi")
                .role(Role.ROLE_USER)
                .emailConfirmed(false)
                .build();
        HttpEntity<CreateConfirmationCodeRequest> httpEntity = new HttpEntity<>(new CreateConfirmationCodeRequest(Type.EMAIL,
                "donaldtrump@gmail.com"));

        doNothing().when(this.emailClient).sendEmail(any(SendEmailRequest.class));
        when(this.userClient.getByEmail("donaldtrump@gmail.com")).thenReturn(userDto);

        ResponseEntity<String> responseEntity = this.testRestTemplate.exchange(
                "/api/v1/confirmation-codes/regeneration",
                HttpMethod.POST,
                httpEntity,
                String.class
        );

        System.out.println(responseEntity);

        Assertions.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        Assertions.assertEquals("Код подтверждения успешно отправлен на электронную почту", responseEntity.getBody());
    }

    @Test
    public void updatePassword_ValidCodeAndPasswords_UpdatesPassword() {
        UserDto userDto = UserDto.builder()
                .id(4L)
                .firstName("Pavel")
                .lastName("Durov")
                .email("paveldurovtg@gmail.com")
                .password("$2a$12$5AvRdljjFvz1gJtVioGOJ./tAV8KHjln/fvKjrRXMAUxxqjYN4Vpi")
                .role(Role.ROLE_USER)
                .emailConfirmed(false)
                .build();
        HttpEntity<UpdatePasswordRequest> httpEntity = new HttpEntity<>(new UpdatePasswordRequest("12345678", "12345678",
                new CodeVerificationRequest("paveldurovtg@gmail.com", 1_234_567)));

        doNothing().when(this.authClient).logoutOnAllDevices(anyString(), anyString());
        when(this.userClient.getByEmail("paveldurovtg@gmail.com")).thenReturn(userDto);

        ResponseEntity<String> responseEntity = this.testRestTemplate.exchange(
                "/api/v1/confirmation-codes/password-reset",
                HttpMethod.POST,
                httpEntity,
                String.class
        );

        System.out.println(responseEntity.getBody());

        Assertions.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        Assertions.assertEquals("Пароль успешно обновлен", responseEntity.getBody());
    }

    @Test
    public void create_InvalidEmail_ReturnsValidationError() {
        HttpEntity<CreateConfirmationCodeRequest> httpEntity = new HttpEntity<>(new CreateConfirmationCodeRequest(Type.EMAIL,
                "invalid-email"));

        ResponseEntity<ProblemDetail> responseEntity = this.testRestTemplate.exchange(
                "/api/v1/confirmation-codes",
                HttpMethod.POST,
                httpEntity,
                ProblemDetail.class
        );

        Assertions.assertTrue(responseEntity.getStatusCode().is4xxClientError());
        Assertions.assertEquals("Электронная почта должна быть валидной", responseEntity.getBody().getDetail());
    }

    @Test
    public void confirmEmail_InvalidCode_ReturnsError() {
        UserDto userDto = UserDto.builder()
                .id(6L)
                .firstName("Tom")
                .lastName("Holland")
                .email("tomholland@gmail.com")
                .password("$2a$12$5AvRdljjFvz1gJtVioGOJ./tAV8KHjln/fvKjrRXMAUxxqjYN4Vpi")
                .role(Role.ROLE_USER)
                .emailConfirmed(false)
                .build();

        HttpEntity<CodeVerificationRequest> httpEntity = new HttpEntity<>(new CodeVerificationRequest("tomholland@gmail.com", 1111111));

        when(this.userClient.getByEmail("tomholland@gmail.com")).thenReturn(userDto);

        ResponseEntity<ProblemDetail> responseEntity = this.testRestTemplate.exchange(
                "/api/v1/confirmation-codes/email/verify",
                HttpMethod.POST,
                httpEntity,
                ProblemDetail.class
        );

        Assertions.assertTrue(responseEntity.getStatusCode().is4xxClientError());
        Assertions.assertEquals("Код подтверждения не валиден(неправильный код или срок годности истёк)", responseEntity.getBody().getDetail());
    }

    @Test
    public void updatePassword_PasswordsDoNotMatch_ReturnsError() {
        UserDto userDto = UserDto.builder()
                .id(8L)
                .firstName("Ryan")
                .lastName("Gosling")
                .email("ryangosling@gmail.com")
                .password("$2a$12$5AvRdljjFvz1gJtVioGOJ./tAV8KHjln/fvKjrRXMAUxxqjYN4Vpi")
                .role(Role.ROLE_USER)
                .emailConfirmed(false)
                .build();

        HttpEntity<UpdatePasswordRequest> httpEntity = new HttpEntity<>(new UpdatePasswordRequest("12345678", "87654321",
                new CodeVerificationRequest("ryangosling@gmail.com", 1_234_567)));

        when(this.userClient.getByEmail("ryangosling@gmail.com")).thenReturn(userDto);

        ResponseEntity<ProblemDetail> responseEntity = this.testRestTemplate.exchange(
                "/api/v1/confirmation-codes/password-reset",
                HttpMethod.POST,
                httpEntity,
                ProblemDetail.class
        );

        System.out.println(responseEntity);
        Assertions.assertTrue(responseEntity.getStatusCode().is4xxClientError());
    }

    @Test
    public void recreate_NoExistingCode_ReturnsError() {
        UserDto userDto = UserDto.builder()
                .id(7L)
                .firstName("Toby")
                .lastName("Macgyver")
                .email("tobymacgyver@gmail.com")
                .password("$2a$12$5AvRdljjFvz1gJtVioGOJ./tAV8KHjln/fvKjrRXMAUxxqjYN4Vpi")
                .role(Role.ROLE_USER)
                .emailConfirmed(false)
                .build();
        HttpEntity<CreateConfirmationCodeRequest> httpEntity = new HttpEntity<>(new CreateConfirmationCodeRequest(Type.EMAIL,
                "tobymacgyver@gmail.com"));

        when(this.userClient.getByEmail("tobymacgyver@gmail.com")).thenReturn(userDto);

        ResponseEntity<ProblemDetail> responseEntity = this.testRestTemplate.exchange(
                "/api/v1/confirmation-codes/regeneration",
                HttpMethod.POST,
                httpEntity,
                ProblemDetail.class
        );

        System.out.println(responseEntity);
        Assertions.assertTrue(responseEntity.getStatusCode().is4xxClientError());
        Assertions.assertEquals("Вы не можете пересоздать код подтверждения, так как код не был ещё создан. Создайте код подтверждения",
                responseEntity.getBody().getDetail());
    }

}