package com.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "slot_assignments")
@Getter
@Setter
@NoArgsConstructor
public class SlotAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "screen_category_id", nullable = false)
    private UUID screenCategoryId;

    @Column(name = "slot_position", nullable = false)
    private Integer slotPosition;

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;

    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
