package com.amorim.finance_manager.category.repository;

import com.amorim.finance_manager.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByUserIdOrderByNameAsc(UUID userId);

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    List<Category> findAllByUserIdAndIdIn(UUID userId, Collection<UUID> ids);
}
