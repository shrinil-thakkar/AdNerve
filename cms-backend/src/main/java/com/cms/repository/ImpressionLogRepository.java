package com.cms.repository;

import com.cms.entity.ImpressionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ImpressionLogRepository extends JpaRepository<ImpressionLog, UUID> {

    @Query("""
        SELECT i.mediaId, COUNT(i),
               SUM(CASE WHEN i.completed = true THEN 1 ELSE 0 END)
        FROM ImpressionLog i
        WHERE i.screenId = :screenId
          AND i.playedAt BETWEEN :from AND :to
        GROUP BY i.mediaId
    """)
    List<Object[]> aggregateByMedia(
            @Param("screenId") UUID screenId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query("""
        SELECT i.mediaId, COUNT(i),
               SUM(CASE WHEN i.completed = true THEN 1 ELSE 0 END)
        FROM ImpressionLog i
        WHERE i.screenId = :screenId
          AND i.mediaId = :mediaId
          AND i.playedAt BETWEEN :from AND :to
        GROUP BY i.mediaId
    """)
    List<Object[]> aggregateByMediaFiltered(
            @Param("screenId") UUID screenId,
            @Param("mediaId") UUID mediaId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );
}
