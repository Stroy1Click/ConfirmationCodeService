package ru.stroy1click.confirmationcode;

import org.springframework.boot.SpringApplication;
import ru.stroy1click.confirmationcode.config.TestcontainersConfiguration;

public class TestStroy1ClickConfirmationCodeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(Stroy1ClickConfirmationCodeServiceApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }

}
