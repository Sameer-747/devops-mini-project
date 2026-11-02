package com.devops.taskmanager.controller;

import com.devops.taskmanager.model.Task;
import com.devops.taskmanager.model.TaskRequest;
import com.devops.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<Task> all() {
        return service.findAll();
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return service.stats();
    }

    @GetMapping("/{id}")
    public Task one(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task create(@Valid @RequestBody TaskRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Task update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/toggle")
    public Task toggle(@PathVariable Long id) {
        return service.toggle(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
