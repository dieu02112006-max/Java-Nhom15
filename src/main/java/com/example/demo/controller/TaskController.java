package com.example.demo.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.example.demo.entity.Task;
import com.example.demo.service.TaskService;
import com.example.demo.security.AuthorizationService;
import java.util.List;
import org.springframework.hateoas.EntityModel;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.demo.security.AppUserPrincipal;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;
    private final AuthorizationService authorization;

    public TaskController(TaskService service, AuthorizationService authorization) {
        this.service = service;
        this.authorization = authorization;
    }

    @GetMapping
    public List<Task> findAll() {
        return service.findAll().stream()
                .filter(task -> authorization.canAccessUser(task.getUserId()))
                .toList();
    }

    @GetMapping("/{id}")
    public EntityModel<Task> findById(@PathVariable Long id) {
        Task task = service.findById(id);
        authorization.requireUserAccess(task.getUserId());
        return EntityModel.of(task,
                linkTo(methodOn(TaskController.class).findById(id)).withSelfRel(),
                linkTo(methodOn(TaskController.class).findAll()).withRel("tasks"));
    }

    @PostMapping
    @PreAuthorize("@authz.canAccessUser(#task.userId)")
    public ResponseEntity<Task> create(@RequestBody Task task, @AuthenticationPrincipal AppUserPrincipal principal) {
        if ("blocked".equals(task.getStatus()) && principal.role() != com.example.demo.entity.enums.UserRole.ADMIN) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ quản trị viên mới được chặn công việc");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(task));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.canAccessUser(#task.userId)")
    public Task update(@PathVariable Long id, @RequestBody Task task, @AuthenticationPrincipal AppUserPrincipal principal) {
        Task existing = service.findById(id);
        authorization.requireUserAccess(existing.getUserId());
        if (principal.role() != com.example.demo.entity.enums.UserRole.ADMIN
                && ("blocked".equals(existing.getStatus()) || "blocked".equals(task.getStatus()))) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ quản trị viên mới được quản lý công việc bị chặn");
        }
        return service.update(id, task);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        Task task = service.findById(id);
        authorization.requireUserAccess(task.getUserId());
        if ("blocked".equals(task.getStatus()) && principal.role() != com.example.demo.entity.enums.UserRole.ADMIN) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ quản trị viên mới được xóa công việc bị chặn");
        }
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
