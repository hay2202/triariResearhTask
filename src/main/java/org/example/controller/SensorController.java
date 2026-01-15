package org.example.controller;

import jakarta.validation.Valid;
import org.example.model.SensorData;
import org.example.service.SensorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/sensors")
public class SensorController {

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @PostMapping("/data")
    public ResponseEntity<Void> ingestData(@Valid @RequestBody SensorData data) {
        sensorService.ingestData(data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{sensor_id}/data")
    public ResponseEntity<SensorData> getLatestReading(@PathVariable("sensor_id") String sensorId) {
        if (sensorId == null || sensorId.trim().isEmpty()) {
             throw new IllegalArgumentException("Sensor ID cannot be empty");
        }
        SensorData data = sensorService.getLatestReading(sensorId);
        if (data != null) {
            return ResponseEntity.ok(data);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{sensor_id}/data/range")
    public ResponseEntity<List<SensorData>> getReadingsInRange(
            @PathVariable("sensor_id") String sensorId,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        
        if (sensorId == null || sensorId.trim().isEmpty()) {
             throw new IllegalArgumentException("Sensor ID cannot be empty");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start time cannot be after End time");
        }

        return ResponseEntity.ok(sensorService.getReadingsInRange(sensorId, start, end));
    }

    @GetMapping
    public ResponseEntity<Set<Object>> listSensors() {
        return ResponseEntity.ok(sensorService.getAllSensors());
    }
}
