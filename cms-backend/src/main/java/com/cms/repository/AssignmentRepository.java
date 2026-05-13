package com.cms.repository;

import com.cms.entity.SlotAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<SlotAssignment, UUID> {

    List<SlotAssignment> findBySlotIdAndIsActiveTrueOrderByPriority(UUID slotId);

    List<SlotAssignment> findBySlotId(UUID slotId);

    Optional<SlotAssignment> findByIdAndSlotId(UUID id, UUID slotId);

    boolean existsBySlotIdAndMediaIdAndIsActiveTrue(UUID slotId, UUID mediaId);
}
