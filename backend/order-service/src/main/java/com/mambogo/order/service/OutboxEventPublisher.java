package com.mambogo.order.service;

import com.mambogo.order.entity.OutboxEvent;
import com.mambogo.order.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class OutboxEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(OutboxEventPublisher.class);
    
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    /**
     * Publish pending events every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void publishPendingEvents() {
        logger.info("Starting to publish pending outbox events...");
        
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents();
        List<OutboxEvent> retryEvents = outboxEventRepository.findReadyForRetryEvents(LocalDateTime.now());
        
        logger.info("Found {} pending events and {} retry events", pendingEvents.size(), retryEvents.size());
        
        // Process pending events
        for (OutboxEvent event : pendingEvents) {
            publishEvent(event);
        }
        
        // Process retry events
        for (OutboxEvent event : retryEvents) {
            publishEvent(event);
        }
    }
    
    /**
     * Publish a single event to Kafka
     */
    private void publishEvent(OutboxEvent event) {
        try {
            String topic = getTopicForEventType(event.getEventType());
            
            logger.info("Publishing event {} to topic {}", event.getId(), topic);
            
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, event.getPayload());
            
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    // Success
                    logger.info("Successfully published event {} to topic {}", event.getId(), topic);
                    event.markAsSent();
                    outboxEventRepository.save(event);
                } else {
                    // Failure
                    logger.error("Failed to publish event {} to topic {}: {}", event.getId(), topic, throwable.getMessage());
                    handlePublishFailure(event);
                }
            });
            
        } catch (Exception e) {
            logger.error("Error publishing event {}: {}", event.getId(), e.getMessage(), e);
            handlePublishFailure(event);
        }
    }
    
    /**
     * Handle publish failure with retry logic
     */
    private void handlePublishFailure(OutboxEvent event) {
        event.incrementRetryCount();
        outboxEventRepository.save(event);
        
        if (event.getStatus() == OutboxEvent.OutboxStatus.FAILED) {
            logger.error("Event {} has exceeded max retries and is marked as failed", event.getId());
            // Could trigger alerting or dead letter queue processing here
        }
    }
    
    /**
     * Get Kafka topic for event type
     */
    private String getTopicForEventType(String eventType) {
        switch (eventType) {
            case "OrderCreated":
                return "order-events";
            case "PaymentAuthorized":
                return "payment-events";
            case "PaymentFailed":
                return "payment-events";
            default:
                return "general-events";
        }
    }
    
    /**
     * Clean up old sent events (run daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldEvents() {
        logger.info("Starting cleanup of old sent events...");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7); // Keep events for 7 days
        outboxEventRepository.deleteOldSentEvents(cutoffDate);
        
        logger.info("Cleanup completed");
    }
    
    /**
     * Get outbox statistics for monitoring
     */
    public OutboxStats getOutboxStats() {
        return new OutboxStats(
            outboxEventRepository.countPendingEvents(),
            outboxEventRepository.countRetryEvents(),
            outboxEventRepository.countFailedEvents()
        );
    }
    
    /**
     * Statistics class for monitoring
     */
    public static class OutboxStats {
        private final long pendingCount;
        private final long retryCount;
        private final long failedCount;
        
        public OutboxStats(long pendingCount, long retryCount, long failedCount) {
            this.pendingCount = pendingCount;
            this.retryCount = retryCount;
            this.failedCount = failedCount;
        }
        
        public long getPendingCount() {
            return pendingCount;
        }
        
        public long getRetryCount() {
            return retryCount;
        }
        
        public long getFailedCount() {
            return failedCount;
        }
    }
}
