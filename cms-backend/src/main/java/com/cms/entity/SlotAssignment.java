package com.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "slot_assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"slot_id", "media_id"}))
@Getter
@Setter
@NoArgsConstructor
public class SlotAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    private Integer priority = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    @PrePersist
    void prePersist() {
        assignedAt = OffsetDateTime.now();
    }
}
