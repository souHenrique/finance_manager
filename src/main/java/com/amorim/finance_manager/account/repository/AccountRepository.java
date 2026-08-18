package com.amorim.finance_manager.account.repository;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);
    List<Account> findAllByUserId(UUID userId);

    UUID user(User user);
}