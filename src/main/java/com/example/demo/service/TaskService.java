package com.example.demo.service;

import com.example.demo.entity.Task;
import com.example.demo.entity.TaskList;
import com.example.demo.repository.TaskListRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.TaskTagRepository;
import com.example.demo.repository.ReminderRepository;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskService {

    private static final Set<String> STATUSES = Set.of("todo", "in_progress", "done");
    private static final Set<String> PRIORITIES = Set.of("low", "medium", "high");

    private final TaskRepository repository;
    private final TaskListRepository taskListRepository;
    private final TaskTagRepository taskTagRepository;
    private final ReminderRepository reminderRepository;

    public TaskService(
            TaskRepository repository,
            TaskListRepository taskListRepository,
            TaskTagRepository taskTagRepository,
            ReminderRepository reminderRepository) {
        this.repository = repository;
        this.taskListRepository = taskListRepository;
        this.taskTagRepository = taskTagRepository;
        this.reminderRepository = reminderRepository;
    }

    public List<Task> findAll() {
        return repository.findAll();
    }

    public Task findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    public Task create(Task task) {
        validate(task, null);
        task.setId(null);
        return repository.save(task);
    }

    public Task update(Long id, Task task) {
        findById(id);
        validate(task, id);
        task.setId(id);
        return repository.save(task);
    }

    public void delete(Long id) {
        if (repository.existsByParentTaskId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delete subtasks before deleting the parent task");
        }
        if (taskTagRepository.existsByTaskId(id) || reminderRepository.existsByTaskId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delete task tags and reminders before deleting the task");
        }
        if (!repository.deleteById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
    }

    private void validate(Task task, Long updatingId) {
        if (task.getUserId() == null || task.getListId() == null
                || task.getTitle() == null || task.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId, listId and title are required");
        }
        if (task.getStatus() == null || !STATUSES.contains(task.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be todo, in_progress or done");
        }
        if (task.getPriority() == null || !PRIORITIES.contains(task.getPriority())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "priority must be low, medium or high");
        }

        TaskList taskList = taskListRepository.findById(task.getListId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task list not found"));
        if (!task.getUserId().equals(taskList.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task and task list must belong to the same user");
        }
        if (task.getParentTaskId() != null) {
            if (task.getParentTaskId().equals(updatingId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A task cannot be its own parent");
            }
            Task parent = repository.findById(task.getParentTaskId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent task not found"));
            if (!task.getUserId().equals(parent.getUserId())
                    || !task.getListId().equals(parent.getListId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent task must belong to the same user and list");
            }
        }
        if (task.getEstimatedCost() != null && task.getEstimatedCost().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "estimatedCost cannot be negative");
        }
    }
}
