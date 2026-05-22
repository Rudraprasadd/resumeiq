package com.resumeiq.repository;

import com.resumeiq.model.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {

    List<Analysis> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Used to check Redis cache key before calling the AI provider */
    Optional<Analysis> findByAiCacheKey(String cacheKey);

    /** Count analyses this month — for FREE tier quota enforcement */
    @Query("""
        SELECT COUNT(a) FROM Analysis a
        WHERE a.user.id = :userId
        AND a.createdAt >= :from
        AND a.createdAt < :to
    """)
    long countByUserIdAndCreatedAtBetween(
        @Param("userId") UUID userId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}
