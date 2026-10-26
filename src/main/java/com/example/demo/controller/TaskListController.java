package com.example.demo.controller;

import com.example.demo.entity.TaskList;
import com.example.demo.service.TaskListService;
import com.example.demo.security.AuthorizationService;
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
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/task-lists")
public class TaskListController {

    private final TaskListService service;
    private final AuthorizationService authorization;

    public TaskListController(TaskListService service, AuthorizationService authorization) {
        this.service = service;
        this.authorization = authorization;
    }

    @GetMapping
    public List<TaskList> findAll() {
        return service.findAll().stream()
                .filter(taskList -> authorization.canAccessUser(taskList.getUserId()))
                .toList();
    }

    @GetMapping("/{id}")
    public TaskList findById(@PathVariable Long id) {
        TaskList taskList = service.findById(id);
        authorization.requireUserAccess(taskList.getUserId());
        return taskList;
    }

    @PostMapping
    @PreAuthorize("@authz.canAccessUser(#taskList.userId)")
    public ResponseEntity<TaskList> create(@RequestBody TaskList taskList) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(taskList));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.canAccessUser(#taskList.userId)")
    public TaskList update(@PathVariable Long id, @RequestBody TaskList taskList) {
        authorization.requireUserAccess(service.findById(id).getUserId());
        return service.update(id, taskList);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authorization.requireUserAccess(service.findById(id).getUserId());
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
