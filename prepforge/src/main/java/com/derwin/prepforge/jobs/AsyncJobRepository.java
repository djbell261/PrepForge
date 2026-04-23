package com.derwin.prepforge.jobs;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface AsyncJobRepository extends JpaRepository<AsyncJob, UUID> {
    Optional<AsyncJob> findByIdempotencyKey(String idempotencyKey);

    long countByStatusIn(Collection<AsyncJobStatus> statuses);

    long countByJobTypeAndStatusIn(AsyncJobType jobType, Collection<AsyncJobStatus> statuses);

    Optional<AsyncJob> findTopByAggregateTypeAndAggregateIdAndJobTypeOrderByCreatedAtDesc(
            AsyncJobAggregateType aggregateType,
            UUID aggregateId,
            AsyncJobType jobType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select job from AsyncJob job
            where job.status in :statuses
              and job.scheduledAt <= :now
            order by job.scheduledAt asc, job.createdAt asc
            """)
    List<AsyncJob> findEligibleJobsForUpdate(Collection<AsyncJobStatus> statuses, Instant now, Pageable pageable);
}
