package org.example.service;

import org.example.model.ScalingRecommendation;
import org.example.model.Worker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScalingService {

    private final WorkerService workerService;
    private final ThroughputMonitor throughputMonitor;

    @Value("${scaling.worker.capacity:1500}")
    private int workerCapacity;

    @Value("${scaling.worker.min:1}")
    private int minWorkers;

    @Value("${scaling.worker.max:10}")
    private int maxWorkers;

    public ScalingService(WorkerService workerService, ThroughputMonitor throughputMonitor) {
        this.workerService = workerService;
        this.throughputMonitor = throughputMonitor;
    }

    public ScalingRecommendation getScalingRecommendation() {
        long currentThroughput = throughputMonitor.getCurrentThroughput();
        List<Worker> workers = workerService.getAllWorkers();
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
}
