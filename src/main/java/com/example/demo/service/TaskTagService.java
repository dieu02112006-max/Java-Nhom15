package com.example.demo.service;

import com.example.demo.model.Tag;
import com.example.demo.model.Task;
import com.example.demo.model.TaskTag;
import com.example.demo.repository.TagRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.TaskTagRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskTagService {

    private final TaskTagRepository repository;
    private final TaskRepository taskRepository;
    private final TagRepository tagRepository;

    public TaskTagService(
            TaskTagRepository repository,
            TaskRepository taskRepository,
            TagRepository tagRepository) {
        this.repository = repository;
        this.taskRepository = taskRepository;
        this.tagRepository = tagRepository;
    }

    public List<TaskTag> findAll() {
        return repository.findAll();
    }

    public TaskTag findById(Long taskId, Long tagId) {
        return repository.findById(taskId, tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task-tag link not found"));
    }

    public TaskTag create(TaskTag taskTag) {
        if (taskTag.taskId() == null || taskTag.tagId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taskId and tagId are required");
        }
        Task task = taskRepository.findById(taskTag.taskId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task not found"));
        Tag tag = tagRepository.findById(taskTag.tagId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag not found"));
        if (!task.getUserId().equals(tag.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task and tag must belong to the same user");
        }
        if (repository.findById(taskTag.taskId(), taskTag.tagId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task-tag link already exists");
        }
        return repository.save(taskTag);
    }

    public void delete(Long taskId, Long tagId) {
        if (!repository.deleteById(taskId, tagId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task-tag link not found");
        }
    }
}
