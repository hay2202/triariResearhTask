package org.example.controller;

import org.example.model.Worker;
import org.example.service.WorkerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workers")
public class WorkerController {

    private final WorkerService workerService;

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @GetMapping
    public ResponseEntity<List<Worker>> listWorkers() {
        return ResponseEntity.ok(workerService.getAllWorkers());
    }

    @PostMapping
    public ResponseEntity<Worker> registerWorker(@RequestBody Worker worker) {
        return ResponseEntity.ok(workerService.registerWorker(worker));
    }

    @DeleteMapping("/{worker_id}")
    public ResponseEntity<Void> deregisterWorker(@PathVariable("worker_id") String workerId) {
        if (workerId == null || workerId.trim().isEmpty()) {
             throw new IllegalArgumentException("Worker ID cannot be empty");
        }
        workerService.deregisterWorker(workerId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{worker_id}/health")
    public ResponseEntity<Worker> updateHealth(@PathVariable("worker_id") String workerId, @RequestBody Map<String, Long> payload) {
        if (workerId == null || workerId.trim().isEmpty()) {
             throw new IllegalArgumentException("Worker ID cannot be empty");
        }
        Long processedCount = payload.getOrDefault("processed_count", 0L);
        Worker worker = workerService.updateHealth(workerId, processedCount);
        if (worker != null) {
            return ResponseEntity.ok(worker);
        }
        return ResponseEntity.notFound().build();
    }
}
