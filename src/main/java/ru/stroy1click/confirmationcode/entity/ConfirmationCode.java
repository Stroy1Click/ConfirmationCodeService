package ru.stroy1click.confirmationcode.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@Table(schema = "confirmation_code", name = "confirmation_codes")
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer code;

    private LocalDateTime expirationDate;

    @Enumerated(value = EnumType.STRING)
    private Type type;

    private String userEmail;
}
