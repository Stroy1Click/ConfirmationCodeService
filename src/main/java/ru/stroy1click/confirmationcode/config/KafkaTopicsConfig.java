package ru.stroy1click.confirmationcode.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic sendEmailCommandsTopic(){
        return TopicBuilder.name("send-email-commands")
                .replicas(1)
                .partitions(3)
                .build();
    }

    @Bean
    public NewTopic logoutOnAllDevicesCommandsTopic(){
        return TopicBuilder.name("logout-on-all-devices-commands")
                .replicas(1)
                .partitions(3)
                .build();
    }

    @Bean
    public NewTopic updatePasswordCommandsTopic(){
        return TopicBuilder.name("update-password-commands")
                .replicas(1)
                .partitions(3)
                .build();
    }

    @Bean
    public NewTopic confirmEmailCommandsTopic(){
        return TopicBuilder.name("confirm-email-commands")
                .replicas(1)
                .partitions(3)
                .build();
    }

    @Bean
    public NewTopic updatePasswordCodeConfirmedEventsTopic(){
        return TopicBuilder.name("update-password-code-confirmed-events")
                .replicas(1)
                .partitions(3)
                .build();
    }
}
