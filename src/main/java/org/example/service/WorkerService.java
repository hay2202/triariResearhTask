package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.ScalingRecommendation;
import org.example.model.Worker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkerService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> valueOperations;
    private final SetOperations<String, Object> setOperations;
    private final ThroughputMonitor throughputMonitor;
    private final ObjectMapper objectMapper;

    private static final String WORKER_KEY_PREFIX = "worker:";
    private static final String WORKER_INDEX_KEY = "workers:index";

    @Value("${scaling.worker.capacity:1500}")
    private int workerCapacity;

    @Value("${scaling.worker.min:1}")
    private int minWorkers;

    @Value("${scaling.worker.max:10}")
    private int maxWorkers;

    public WorkerService(RedisTemplate<String, Object> redisTemplate, ThroughputMonitor throughputMonitor, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.valueOperations = redisTemplate.opsForValue();
        this.setOperations = redisTemplate.opsForSet();
        this.throughputMonitor = throughputMonitor;
        this.objectMapper = objectMapper;
    }

    public Worker registerWorker(Worker worker) {
        String workerId = worker.getWorkerId();
        
        String key = WORKER_KEY_PREFIX + workerId;
        valueOperations.set(key, worker);
        setOperations.add(WORKER_INDEX_KEY, workerId);
        
        return worker;
    }

    public void deregisterWorker(String workerId) {
        String key = WORKER_KEY_PREFIX + workerId;
        redisTemplate.delete(key);
        setOperations.remove(WORKER_INDEX_KEY, workerId);
    }

    public Worker updateHealth(String workerId, long processedCount) {
        String key = WORKER_KEY_PREFIX + workerId;
        Object obj = valueOperations.get(key);
        
        if (obj != null) {
            Worker worker = convertToWorker(obj);
            if (worker != null) {
                worker.setLastHeartbeat(Instant.now());
                worker.setProcessedCount(processedCount);
                worker.setStatus("active");
                valueOperations.set(key, worker);
                return worker;
            }
        }
        return null;
    }

    public List<Worker> getAllWorkers() {
        Set<Object> ids = setOperations.members(WORKER_INDEX_KEY);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> keys = ids.stream()
                .map(id -> WORKER_KEY_PREFIX + id)
                .collect(Collectors.toList());

        List<Object> results = valueOperations.multiGet(keys);
        
        if (results == null) {
            return Collections.emptyList();
        }

        return results.stream()
                .filter(Objects::nonNull)
                .map(this::convertToWorker)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public ScalingRecommendation getScalingRecommendation() {
        long currentThroughput = throughputMonitor.getCurrentThroughput();
        List<Worker> workers = getAllWorkers();
        int activeWorkers = workers.size();

        String recommendation = "HOLD";
        int recommendedWorkers = activeWorkers;
        String reason = "Throughput is within the acceptable range.";

        long requiredCapacity = (long) activeWorkers * workerCapacity;
        long lowerThreshold = (long) activeWorkers * 1000;

        if (activeWorkers < minWorkers) {
            recommendation = "SCALE_UP";
            recommendedWorkers = minWorkers;
            reason = "Active workers below minimum threshold";
        } else if (currentThroughput > requiredCapacity && activeWorkers < maxWorkers) {
            recommendation = "SCALE_UP";
            recommendedWorkers = Math.min(maxWorkers, activeWorkers + (int) Math.ceil((currentThroughput - requiredCapacity) / (double) workerCapacity));
            reason = "Throughput exceeds " + workerCapacity + " msg/s per worker";
        } else if (currentThroughput < lowerThreshold && activeWorkers > minWorkers) {
            recommendation = "SCALE_DOWN";
            recommendedWorkers = Math.max(minWorkers, activeWorkers - 1); // Scale down one by one
            reason = "Throughput is below 1000 msg/s per worker";
        }
        
        if(recommendedWorkers == activeWorkers && !recommendation.equals("SCALE_UP")) {
             recommendation = "HOLD";
        }

        return new ScalingRecommendation(currentThroughput, activeWorkers, recommendation, recommendedWorkers, reason);
    }

    private Worker convertToWorker(Object obj) {
        try {
            if (obj instanceof Worker) {
                return (Worker) obj;
            }
            return objectMapper.convertValue(obj, Worker.class);
        } catch (IllegalArgumentException e) {
            // Log error or handle it
            System.err.println("Failed to convert object to Worker: " + obj + " Error: " + e.getMessage());
            return null;
        }
    }
}
