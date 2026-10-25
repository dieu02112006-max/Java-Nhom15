package com.example.demo.service;

import com.example.demo.model.Reminder;
import com.example.demo.model.Task;
import com.example.demo.repository.ReminderRepository;
import com.example.demo.repository.TaskRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReminderService {

    private final ReminderRepository repository;
    private final TaskRepository taskRepository;

    public ReminderService(ReminderRepository repository, TaskRepository taskRepository) {
        this.repository = repository;
        this.taskRepository = taskRepository;
    }

    public List<Reminder> findAll() {
        return repository.findAll();
    }

    public Reminder findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reminder not found"));
    }

    public Reminder create(Reminder reminder) {
        validate(reminder);
        reminder.setId(null);
        return repository.save(reminder);
    }

    public Reminder update(Long id, Reminder reminder) {
        findById(id);
        validate(reminder);
        reminder.setId(id);
        return repository.save(reminder);
    }

    public void delete(Long id) {
        if (!repository.deleteById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reminder not found");
        }
    }

    private void validate(Reminder reminder) {
        if (reminder.getUserId() == null || reminder.getRemindAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId and remindAt are required");
        }
        if (reminder.getTaskId() != null) {
            Task task = taskRepository.findById(reminder.getTaskId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task not found"));
            if (!reminder.getUserId().equals(task.getUserId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reminder and task must belong to the same user");
            }
        }
        if (reminder.getTaskId() == null && reminder.getBudgetId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taskId or budgetId is required");
        }
    }
}
