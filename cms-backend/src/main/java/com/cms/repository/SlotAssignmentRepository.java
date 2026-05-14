package com.cms.repository;

import com.cms.entity.SlotAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlotAssignmentRepository extends JpaRepository<SlotAssignment, UUID> {

    List<SlotAssignment> findByScreenCategoryIdOrderBySlotPosition(UUID screenCategoryId);

    @Query("""
        SELECT a FROM SlotAssignment a
        WHERE a.screenCategoryId = :screenCategoryId
          AND a.slotPosition <= :maxPosition
        ORDER BY a.slotPosition
    """)
    List<SlotAssignment> findActiveAssignments(
            @Param("screenCategoryId") UUID screenCategoryId,
            @Param("maxPosition") Integer maxPosition
    );

    Optional<SlotAssignment> findByScreenCategoryIdAndSlotPosition(
            UUID screenCategoryId, Integer slotPosition
    );

    void deleteByScreenCategoryIdAndSlotPosition(UUID screenCategoryId, Integer slotPosition);
}
