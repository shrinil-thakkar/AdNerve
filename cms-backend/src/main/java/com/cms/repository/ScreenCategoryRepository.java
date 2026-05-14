package com.cms.repository;

import com.cms.entity.CategoryType;
import com.cms.entity.ScreenCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScreenCategoryRepository extends JpaRepository<ScreenCategory, UUID> {

    List<ScreenCategory> findByScreenIdOrderByStartTime(UUID screenId);

    Optional<ScreenCategory> findByScreenIdAndCategory(UUID screenId, CategoryType category);
}
