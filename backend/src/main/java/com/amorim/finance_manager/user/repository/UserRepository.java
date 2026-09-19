package com.amorim.finance_manager.user.repository;

import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndStatus(String email, UserStatus status);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
