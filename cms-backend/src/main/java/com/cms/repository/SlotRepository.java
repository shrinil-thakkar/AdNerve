package com.cms.repository;

import com.cms.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<Slot, UUID> {

    List<Slot> findByScreenIdAndIsActiveTrue(UUID screenId);

    Optional<Slot> findByIdAndScreenIdAndIsActiveTrue(UUID id, UUID screenId);
}
