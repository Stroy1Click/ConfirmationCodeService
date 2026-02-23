package ru.stroy1click.confirmationcode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Stroy1ClickConfirmationCodeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(Stroy1ClickConfirmationCodeServiceApplication.class, args);
    }

}