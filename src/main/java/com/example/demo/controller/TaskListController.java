package com.example.demo.controller;

import com.example.demo.model.TaskList;
import com.example.demo.service.TaskListService;
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
@RequestMapping("/api/task-lists")
public class TaskListController {

    private final TaskListService service;

    public TaskListController(TaskListService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaskList> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public TaskList findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<TaskList> create(@RequestBody TaskList taskList) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(taskList));
    }

    @PutMapping("/{id}")
    public TaskList update(@PathVariable Long id, @RequestBody TaskList taskList) {
        return service.update(id, taskList);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
