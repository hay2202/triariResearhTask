package org.example.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MetricsService {

    private final ThroughputMonitor throughputMonitor;

    public MetricsService(ThroughputMonitor throughputMonitor) {
        this.throughputMonitor = throughputMonitor;
    }

    public Map<String, Long> getThroughput() {
        return Map.of("current_throughput", throughputMonitor.getCurrentThroughput());
    }
}
