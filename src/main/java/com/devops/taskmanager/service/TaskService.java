package com.devops.taskmanager.service;

import com.devops.taskmanager.model.Task;
import com.devops.taskmanager.model.TaskRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Business logic for tasks. Data is kept in memory so the app has no
 * external dependencies and can be deployed anywhere by the pipelines.
 */
@Service
public class TaskService {

    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public TaskService() {
        create(new TaskRequest("Configure Jenkins pipeline", Task.Priority.HIGH, null));
        create(new TaskRequest("Write Ansible playbook", Task.Priority.MEDIUM, null));
        create(new TaskRequest("Set up Azure Pipelines", Task.Priority.LOW, null));
    }

    public List<Task> findAll() {
        return tasks.values().stream()
                .sorted(Comparator.comparing(Task::getId))
                .toList();
    }

    public Task findById(Long id) {
        Task task = tasks.get(id);
        if (task == null) {
            throw new TaskNotFoundException(id);
        }
        return task;
    }

    public Task create(TaskRequest request) {
        long id = sequence.incrementAndGet();
        Task.Priority priority = request.priority() != null ? request.priority() : Task.Priority.MEDIUM;
        Task task = new Task(id, request.title().trim(), priority);
        tasks.put(id, task);
        return task;
    }

    public Task update(Long id, TaskRequest request) {
        Task task = findById(id);
        task.setTitle(request.title().trim());
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.completed() != null) {
            task.setCompleted(request.completed());
        }
        return task;
    }

    public Task toggle(Long id) {
        Task task = findById(id);
        task.setCompleted(!task.isCompleted());
        return task;
    }

    public void delete(Long id) {
        if (tasks.remove(id) == null) {
            throw new TaskNotFoundException(id);
        }
    }

    public Map<String, Long> stats() {
        long total = tasks.size();
        long done = tasks.values().stream().filter(Task::isCompleted).count();
        return Map.of("total", total, "completed", done, "pending", total - done);
    }
}
