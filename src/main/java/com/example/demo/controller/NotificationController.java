package com.example.demo.controller;

import com.example.demo.model.NotificationModel;
import com.example.demo.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping public List<NotificationModel> findAll() { return service.findAll(); }
    @GetMapping("/{id}") public NotificationModel findById(@PathVariable Long id) { return service.findById(id); }
    @PostMapping public ResponseEntity<NotificationModel> create(@RequestBody NotificationModel model) { NotificationModel saved = service.create(model); return ResponseEntity.created(linkTo(methodOn(NotificationController.class).findById(saved.id())).toUri()).body(saved); }
    @PutMapping("/{id}") public NotificationModel update(@PathVariable Long id, @RequestBody NotificationModel model) { return service.update(id, model); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { service.delete(id); }
}
