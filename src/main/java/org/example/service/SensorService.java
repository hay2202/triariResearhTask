package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.model.SensorData;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class SensorService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ThroughputMonitor throughputMonitor;

    private static final String SENSOR_KEY_PREFIX = "sensor:";
    private static final String SENSOR_DATA_KEY_SUFFIX = ":data";
    private static final String ALL_SENSORS_KEY = "sensors:all";

    public SensorService(RedisTemplate<String, Object> redisTemplate, ThroughputMonitor throughputMonitor) {
        this.redisTemplate = redisTemplate;
        this.throughputMonitor = throughputMonitor;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void ingestData(SensorData data) {
        if (data.getTimestamp() == null) {
            data.setTimestamp(Instant.now());
        }

        String key = SENSOR_KEY_PREFIX + data.getSensorId() + SENSOR_DATA_KEY_SUFFIX;
        
        // Store in ZSET for time-range queries (Score = timestamp epoch milli)
        redisTemplate.opsForZSet().add(key, data, data.getTimestamp().toEpochMilli());
        
        // Add to set of all sensors
        redisTemplate.opsForSet().add(ALL_SENSORS_KEY, data.getSensorId());

        // Record throughput
        throughputMonitor.increment();
    }

    public SensorData getLatestReading(String sensorId) {
        String key = SENSOR_KEY_PREFIX + sensorId + SENSOR_DATA_KEY_SUFFIX;
        Set<Object> result = redisTemplate.opsForZSet().reverseRange(key, 0, 0);
        if (result != null && !result.isEmpty()) {
            Object obj = result.iterator().next();
            if (obj instanceof SensorData) {
                return (SensorData) obj;
            } else {
                 return objectMapper.convertValue(obj, SensorData.class);
            }
        }
        return null;
    }

    public List<SensorData> getReadingsInRange(String sensorId, Instant start, Instant end) {
        String key = SENSOR_KEY_PREFIX + sensorId + SENSOR_DATA_KEY_SUFFIX;
        Set<Object> result = redisTemplate.opsForZSet().rangeByScore(key, start.toEpochMilli(), end.toEpochMilli());
        
        List<SensorData> readings = new ArrayList<>();
        if (result != null) {
            for (Object obj : result) {
                 if (obj instanceof SensorData) {
                    readings.add((SensorData) obj);
                } else {
                     readings.add(objectMapper.convertValue(obj, SensorData.class));
                }
            }
        }
        return readings;
    }

    public Set<Object> getAllSensors() {
        return redisTemplate.opsForSet().members(ALL_SENSORS_KEY);
    }
}
