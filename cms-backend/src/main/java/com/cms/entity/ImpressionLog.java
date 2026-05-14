package com.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "impression_logs")
@Getter
@Setter
@NoArgsConstructor
public class ImpressionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "screen_id", nullable = false)
    private UUID screenId;

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;

    @Column(name = "category", length = 20)
    private String category;

    @Column(name = "slot_position")
    private Integer slotPosition;

    @Column(name = "played_at", nullable = false)
    private OffsetDateTime playedAt;

    @Column(name = "duration_played_seconds")
    private Integer durationPlayedSeconds;

    private Boolean completed = false;
}
