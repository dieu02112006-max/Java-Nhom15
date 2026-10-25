package com.example.demo.controller;

import com.example.demo.model.TaskTag;
import com.example.demo.service.TaskTagService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/task-tags")
public class TaskTagController {

    private final TaskTagService service;

    public TaskTagController(TaskTagService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaskTag> findAll() {
        return service.findAll();
    }

    @GetMapping("/{taskId}/{tagId}")
    public TaskTag findById(@PathVariable Long taskId, @PathVariable Long tagId) {
        return service.findById(taskId, tagId);
    }

    @PostMapping
    public ResponseEntity<TaskTag> create(@RequestBody TaskTag taskTag) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(taskTag));
    }

    @DeleteMapping("/{taskId}/{tagId}")
    public ResponseEntity<Void> delete(@PathVariable Long taskId, @PathVariable Long tagId) {
        service.delete(taskId, tagId);
        return ResponseEntity.noContent().build();
    }
}
