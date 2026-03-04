package ru.stroy1click.confirmationcode.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.stroy1click.confirmationcode.client.UserClient;
import ru.stroy1click.confirmationcode.config.TestcontainersConfiguration;
import ru.stroy1click.confirmationcode.dto.*;
import ru.stroy1click.confirmationcode.entity.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Import({TestcontainersConfiguration.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfirmationCodeControllerIT {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @MockitoBean
    private UserClient userClient;

    @Test
    @Order(1)
    public void create_WhenValidDataProvidedAndCodeDoesNotExist_CreatesConfirmationCode() {
        //Arrange
        UserDto userDto = UserDto.builder()
                .id(1L)
                .firstName("Ryan")
                .lastName("Thompson")
                .email("rayan_thompson@gmail.com")
                .password("$2a$12$5AvRdljjFvz1gJtVioGOJ./tAV8KHjln/fvKjrRXMAUxxqjYN4Vpi")
                .role(Role.ROLE_USER)
                .isEmailConfirmed(false)
                .build();
        HttpEntity<CreateConfirmationCodeRequest> httpEntity = new HttpEntity<>(new CreateConfirmationCodeRequest(Type.EMAIL,
                "rayan_thompson@gmail.com"));
        when(this.userClient.getByEmail("rayan_thompson@gmail.com")).thenReturn(userDto);

        //Act
        ResponseEntity<String> responseEntity = this.testRestTemplate.exchange(
                "/api/v1/confirmation-codes",
                HttpMethod.POST,
                httpEntity,
                String.class
        );

        //Assert
        assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        assertEquals("Код подтверждения успешно отправлен на электронную почту", responseEntity.getBody());
    }

    @Test
    @Order(3)
    public void confirmEmail_WhenValidDataProvidedAndCodeExists_ConfirmsEmail() {
        //Arrange
        UserDto userDto = UserDto.builder()
                .id(2L)
                .firstName("Jeff")
                .lastName("Bezos")
                .email("jeffbezos@gmail.com")
                .password("$2a$12$5AvRdljjFvz1gJtVioGOJ./tAV8KHjln/fvKjrRXMAUxxqjYN4Vpi")
                .role(Role.ROLE_USER)
                .isEmailConfirmed(false)
                .build();
        HttpEntity<CodeVerificationRequest> httpEntity = new HttpEntity<>(new CodeVerificationRequest("jeffbezos@gmail.com", 1_234_567));
        when(this.userClient.getByEmail("jeffbezos@gmail.com")).thenReturn(userDto);

        //Act
        ResponseEntity<String> responseEntity = this.testRestTemplate.exchange(
                "/api/v1/confirmation-codes/email/verify",
                HttpMethod.POST,
                httpEntity,
                String.class
        );

        //Assert
        assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        assertEquals("Электронная почта подтверждена", responseEntity.getBody());
    }

    @Test
    @Order(2)
    public void recreate_WhenValidDataProvidedAndCodeExists_RegeneratesConfirmationCode() {
        //Arrange
        UserDto userDto = UserDto.builder()
                .id(3L)
                .firstName("Donald")
                .lastName("Trump")
                .email("donaldtrump@gmail.com")
                .password("$2a$12$5AvRdljjFvz1gJtVioGOJ./tAV8KHjln/fvKjrRXMAUxxqjYN4Vpi")
                .role(Role.ROLE_USER)
                .isEmailConfirmed(false)
                .build();
        HttpEntity<CreateConfirmationCodeRequest> httpEntity = new HttpEntity<>(new CreateConfirmationCodeRequest(Type.EMAIL,
                "donaldtrump@gmail.com"));
        when(this.userClient.getByEmail("donaldtrump@gmail.com")).thenReturn(userDto);

        //Act
        ResponseEntity<String> responseEntity = this.testRestTemplate.exchange(
                "/api/v1/confirmation-codes/regeneration",
                HttpMethod.POST,
                httpEntity,
                String.class
        );

        //Assert
        System.out.println(responseEntity.getBody());
        assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        assertEquals("Код подтверждения успешно отправлен на электронную почту",responseEntity.getBody());
    }

    @Test
    public void updatePassword_ValidDataProvidedAndCodeExists_UpdatesPassword() {
        //Arrange
        UserDto userDto = UserDto.builder()
                .id(4L)
                .firstName("Pavel")
                .lastName("Durov")
                .email("paveldurovtg@gmail.com")
                .password("$2a$12$5AvRdljjFvz1gJtVioGOJ./tAV8KHjln/fvKjrRXMAUxxqjYN4Vpi")
                .role(Role.ROLE_USER)
                .isEmailConfirmed(false)
                .build();
        HttpEntity<UpdatePasswordRequest> httpEntity = new HttpEntity<>(new UpdatePasswordRequest("12345678", "12345678",
                new CodeVerificationRequest("paveldurovtg@gmail.com", 1_234_567)));
        when(this.userClient.getByEmail("paveldurovtg@gmail.com")).thenReturn(userDto);

        //Act
        ResponseEntity<String> responseEntity = this.testRestTemplate.exchange(
                "/api/v1/confirmation-codes/password-reset",
                HttpMethod.POST,
                httpEntity,
                String.class
        );

        //Assert
        assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        assertEquals("Пароль успешно обновлен", responseEntity.getBody());
    }

    @Test
    public void verifyEmail_WhenInvalidCodeProvided_ThrowValidationException() {
        //Arrange
        HttpEntity<CodeVerificationRequest> httpEntity = new HttpEntity<>(new CodeVerificationRequest("tomholland@gmail.com", 1111111));

        //Act
        ResponseEntity<ProblemDetail> responseEntity = this.testRestTemplate.exchange(
                "/api/v1/confirmation-codes/email/verify",
                HttpMethod.POST,
                httpEntity,
                ProblemDetail.class
        );

        //Assert
        assertTrue(responseEntity.getStatusCode().is4xxClientError());
    }

    @Test
    public void recreate_WhenCodeDoesNotExists_ShouldThrowValidationException() {
        //Arrange
        UserDto userDto = UserDto.builder()
                .id(7L)
                .firstName("Toby")
                .lastName("Macgyver")
                .email("tobymacgyver@gmail.com")
                .password("$2a$12$5AvRdljjFvz1gJtVioGOJ./tAV8KHjln/fvKjrRXMAUxxqjYN4Vpi")
                .role(Role.ROLE_USER)
                .isEmailConfirmed(false)
                .build();
        HttpEntity<CreateConfirmationCodeRequest> httpEntity = new HttpEntity<>(new CreateConfirmationCodeRequest(Type.EMAIL,
                "tobymacgyver@gmail.com"));
        when(this.userClient.getByEmail("tobymacgyver@gmail.com")).thenReturn(userDto);

        //Act
        ResponseEntity<ProblemDetail> responseEntity = this.testRestTemplate.exchange(
                "/api/v1/confirmation-codes/regeneration",
                HttpMethod.POST,
                httpEntity,
                ProblemDetail.class
        );

        //Assert
        assertTrue(responseEntity.getStatusCode().is4xxClientError());
        Assertions.assertNotNull(responseEntity.getBody());
        assertEquals("Вы не можете пересоздать код подтверждения, так как код не был ещё создан. Создайте код подтверждения",
                responseEntity.getBody().getDetail());
    }

    @Test
    public void updatePassword_PasswordsDoNotMatch_ReturnsError() {
        //Arrange
        UserDto userDto = UserDto.builder()
                .id(8L)
                .firstName("Ryan")
                .lastName("Gosling")
                .email("ryangosling@gmail.com")
                .password("$2a$12$5AvRdljjFvz1gJtVioGOJ./tAV8KHjln/fvKjrRXMAUxxqjYN4Vpi")
                .role(Role.ROLE_USER)
                .isEmailConfirmed(false)
                .build();
        HttpEntity<UpdatePasswordRequest> httpEntity = new HttpEntity<>(new UpdatePasswordRequest("12345678", "87654321",
                new CodeVerificationRequest("ryangosling@gmail.com", 1_234_567)));
        when(this.userClient.getByEmail("ryangosling@gmail.com")).thenReturn(userDto);

        //Act
        ResponseEntity<ProblemDetail> responseEntity = this.testRestTemplate.exchange(
                "/api/v1/confirmation-codes/password-reset",
                HttpMethod.POST,
                httpEntity,
                ProblemDetail.class
        );

        //Assert
        Assertions.assertTrue(responseEntity.getStatusCode().is4xxClientError());
    }

}