package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Worker implements Serializable {
    @JsonProperty("worker_id")
    private String workerId;
    
    private String status;
    
    @JsonProperty("registered_at")
    private Instant registeredAt;
    
    @JsonProperty("last_heartbeat")
    private Instant lastHeartbeat;
    
    @JsonProperty("processed_count")
    private long processedCount;
}
