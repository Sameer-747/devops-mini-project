package com.devops.taskmanager.service;

import com.devops.taskmanager.model.Task;
import com.devops.taskmanager.model.TaskRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskServiceTest {

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(); // starts with 3 sample tasks
    }

    @Test
    @DisplayName("Starts with three sample tasks")
    void seedsSampleTasks() {
        assertThat(service.findAll()).hasSize(3);
    }

    @Test
    @DisplayName("Creates a task with trimmed title and default priority")
    void createDefaultsPriority() {
        Task task = service.create(new TaskRequest("  Run unit tests  ", null, null));

        assertThat(task.getId()).isEqualTo(4L);
        assertThat(task.getTitle()).isEqualTo("Run unit tests");
        assertThat(task.getPriority()).isEqualTo(Task.Priority.MEDIUM);
        assertThat(task.isCompleted()).isFalse();
        assertThat(task.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Updates title, priority and completed flag")
    void updateTask() {
        Task updated = service.update(1L, new TaskRequest("Renamed", Task.Priority.LOW, true));

        assertThat(updated.getTitle()).isEqualTo("Renamed");
        assertThat(updated.getPriority()).isEqualTo(Task.Priority.LOW);
        assertThat(updated.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("Toggle flips the completed flag")
    void toggleTask() {
        assertThat(service.toggle(2L).isCompleted()).isTrue();
        assertThat(service.toggle(2L).isCompleted()).isFalse();
    }

    @Test
    @DisplayName("Delete removes the task")
    void deleteTask() {
        service.delete(3L);
        assertThat(service.findAll()).extracting(Task::getId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("Unknown id throws TaskNotFoundException")
    void unknownIdThrows() {
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task 99 not found");
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    @DisplayName("Stats count completed and pending tasks")
    void stats() {
        service.toggle(1L);
        assertThat(service.stats())
                .containsEntry("total", 3L)
                .containsEntry("completed", 1L)
                .containsEntry("pending", 2L);
    }
}
