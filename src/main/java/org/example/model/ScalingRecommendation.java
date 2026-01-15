package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScalingRecommendation {
    @JsonProperty("current_throughput")
    private long currentThroughput;
    
    @JsonProperty("active_workers")
    private int activeWorkers;
    
    @JsonProperty("recommended_action")
    private String recommendedAction;
    
    @JsonProperty("recommended_workers")
    private int recommendedWorkers;
    
    private String reason;
}
