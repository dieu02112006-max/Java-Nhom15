package com.example.demo.service;

import com.example.demo.model.TaskList;
import com.example.demo.repository.TaskListRepository;
import com.example.demo.repository.TaskRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskListService {

    private final TaskListRepository repository;
    private final TaskRepository taskRepository;

    public TaskListService(TaskListRepository repository, TaskRepository taskRepository) {
        this.repository = repository;
        this.taskRepository = taskRepository;
    }

    public List<TaskList> findAll() {
        return repository.findAll();
    }

    public TaskList findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task list not found"));
    }

    public TaskList create(TaskList taskList) {
        validate(taskList);
        taskList.setId(null);
        return repository.save(taskList);
    }

    public TaskList update(Long id, TaskList taskList) {
        findById(id);
        validate(taskList);
        taskList.setId(id);
        return repository.save(taskList);
    }

    public void delete(Long id) {
        if (taskRepository.existsByListId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delete tasks in this list before deleting the list");
        }
        if (!repository.deleteById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task list not found");
        }
    }

    private void validate(TaskList taskList) {
        if (taskList.getUserId() == null || taskList.getName() == null || taskList.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId and name are required");
        }
    }
}
