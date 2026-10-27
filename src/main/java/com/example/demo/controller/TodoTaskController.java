package com.example.demo.controller;

import com.example.demo.dto.request.TodoTaskRequest;
import com.example.demo.dto.response.TodoTaskResponse;
import com.example.demo.dto.response.TodoTaskListResponse;
import com.example.demo.security.AuthorizationService;
import com.example.demo.service.TodoTaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/todo")
public class TodoTaskController {

    private final TodoTaskService service;
    private final AuthorizationService authorization;

    public TodoTaskController(TodoTaskService service, AuthorizationService authorization) {
        this.service = service;
        this.authorization = authorization;
    }

    @GetMapping("/tasks")
    public List<TodoTaskResponse> findAll() {
        return service.findAll(authorization.currentUserId());
    }

    @GetMapping("/task-lists")
    public List<TodoTaskListResponse> findLists() {
        return service.findLists(authorization.currentUserId());
    }

    @PostMapping("/tasks")
    public ResponseEntity<TodoTaskResponse> create(@Valid @RequestBody TodoTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(authorization.currentUserId(), request));
    }

    @PutMapping("/tasks/{id}")
    public TodoTaskResponse update(@PathVariable Long id, @Valid @RequestBody TodoTaskRequest request) {
        return service.update(authorization.currentUserId(), id, request);
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(authorization.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
