package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SensorData implements Serializable {
    @NotBlank(message = "Sensor ID is required")
    @JsonProperty("sensor_id")
    private String sensorId;
    
    private Instant timestamp;
    
    @NotNull(message = "Readings are required")
    private Map<String, Double> readings;
    
    private Map<String, String> metadata;
}
