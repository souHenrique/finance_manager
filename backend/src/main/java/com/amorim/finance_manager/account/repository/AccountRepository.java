package com.amorim.finance_manager.account.repository;

import com.amorim.finance_manager.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
        select coalesce(sum(account.currentBalance), 0)
        from Account account
        where account.userId = :userId
        """)
    BigDecimal sumCurrentBalanceByUserId(@Param("userId") UUID userId);

    List<Account> findAllByUserIdAndIdIn(UUID userId, Collection<UUID> ids);
}
