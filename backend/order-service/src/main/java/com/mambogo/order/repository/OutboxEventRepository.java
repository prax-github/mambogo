package com.mambogo.order.repository;

import com.mambogo.order.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    
    /**
     * Find all pending events that are ready to be sent
     */
    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.status = 'PENDING' ORDER BY oe.createdAt ASC")
    List<OutboxEvent> findPendingEvents();
    
    /**
     * Find all retry events that are ready to be retried
     */
    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.status = 'RETRY' AND (oe.nextRetryAt IS NULL OR oe.nextRetryAt <= :now) ORDER BY oe.createdAt ASC")
    List<OutboxEvent> findReadyForRetryEvents(@Param("now") LocalDateTime now);
    
    /**
     * Find all events for a specific aggregate
     */
    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.aggregateType = :aggregateType AND oe.aggregateId = :aggregateId ORDER BY oe.createdAt ASC")
    List<OutboxEvent> findByAggregate(@Param("aggregateType") String aggregateType, @Param("aggregateId") String aggregateId);
    
    /**
     * Find all events of a specific type
     */
    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.eventType = :eventType ORDER BY oe.createdAt ASC")
    List<OutboxEvent> findByEventType(@Param("eventType") String eventType);
    
    /**
     * Find all failed events
     */
    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.status = 'FAILED' ORDER BY oe.createdAt DESC")
    List<OutboxEvent> findFailedEvents();
    
    /**
     * Count pending events
     */
    @Query("SELECT COUNT(oe) FROM OutboxEvent oe WHERE oe.status = 'PENDING'")
    long countPendingEvents();
    
    /**
     * Count retry events
     */
    @Query("SELECT COUNT(oe) FROM OutboxEvent oe WHERE oe.status = 'RETRY'")
    long countRetryEvents();
    
    /**
     * Count failed events
     */
    @Query("SELECT COUNT(oe) FROM OutboxEvent oe WHERE oe.status = 'FAILED'")
    long countFailedEvents();
    
    /**
     * Delete old sent events (cleanup)
     */
    @Query("DELETE FROM OutboxEvent oe WHERE oe.status = 'SENT' AND oe.sentAt < :cutoffDate")
    void deleteOldSentEvents(@Param("cutoffDate") LocalDateTime cutoffDate);
}
