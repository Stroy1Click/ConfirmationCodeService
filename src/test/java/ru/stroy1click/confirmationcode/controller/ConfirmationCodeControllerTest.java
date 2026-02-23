package ru.stroy1click.confirmationcode.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.stroy1click.confirmationcode.dto.*;
import ru.stroy1click.confirmationcode.entity.Type;
import ru.stroy1click.confirmationcode.service.ConfirmationCodeService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WebMvcTest(controllers = ConfirmationCodeController.class)
public class ConfirmationCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfirmationCodeService confirmationCodeService;

    @Test
    public void create_WhenCreateConfirmationCodeRequestEmailIsInvalid_ShouldThrowValidationException() throws Exception {
        //Arrange
        CreateConfirmationCodeRequest codeRequest = new CreateConfirmationCodeRequest(Type.EMAIL,
                "invalid-email");
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/api/v1/confirmation-codes")
                .content(new ObjectMapper().writeValueAsString(codeRequest))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        //Act
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();
        String string = result.getResponse().getContentAsString();
        ProblemDetail problemDetail = new ObjectMapper().readValue(string, ProblemDetail.class);
        int status = result.getResponse().getStatus();

        //Assert
        assertEquals(400, status);
        assertEquals("Электронная почта должна быть валидной", problemDetail.getDetail());
    }

    @Test
    public void create_WhenCreateConfirmationCodeRequestEmailIsEmpty_ShouldThrowValidationException() throws Exception {
        //Arrange
        CreateConfirmationCodeRequest codeRequest = new CreateConfirmationCodeRequest(Type.EMAIL,
                "");
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/api/v1/confirmation-codes")
                .content(new ObjectMapper().writeValueAsString(codeRequest))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        //Act
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();
        String string = result.getResponse().getContentAsString();
        ProblemDetail problemDetail = new ObjectMapper().readValue(string, ProblemDetail.class);
        int status = result.getResponse().getStatus();

        //Assert
        assertEquals(400, status);
        Assertions.assertNotNull(problemDetail.getDetail());
        assertTrue( problemDetail.getDetail().contains("Электронная почта пользователя не может быть пустой"));
    }

    @Test
    public void create_WhenCreateConfirmationCodeRequestTypeIsEmpty_ShouldThrowValidationException() throws Exception {
        //Arrange
        CreateConfirmationCodeRequest codeRequest = new CreateConfirmationCodeRequest(null,
                "email@gmail.com");
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/api/v1/confirmation-codes")
                .content(new ObjectMapper().writeValueAsString(codeRequest))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        //Act
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();
        String string = result.getResponse().getContentAsString();
        ProblemDetail problemDetail = new ObjectMapper().readValue(string, ProblemDetail.class);
        int status = result.getResponse().getStatus();

        //Assert
        assertEquals(400, status);
        Assertions.assertNotNull(problemDetail.getDetail());
        assertTrue( problemDetail.getDetail().contains("Тип кода подтверждения не может быть пустым"));
    }
}
