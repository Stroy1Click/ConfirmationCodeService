package ru.stroy1click.confirmationcode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.stroy1click.confirmationcode.entity.ConfirmationCode;
import ru.stroy1click.confirmationcode.entity.Type;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfirmationCodeRepository extends JpaRepository<ConfirmationCode, Long> {

    Optional<ConfirmationCode> findByTypeAndUserEmail(Type type, String userEmail);

    void deleteByTypeAndUserEmail(Type type, String userEmail);

    Integer countByTypeAndUserEmail(Type type, String userEmail);

    void deleteByCode(Integer code);
}
