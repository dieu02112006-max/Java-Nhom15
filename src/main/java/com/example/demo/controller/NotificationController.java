package com.example.demo.controller;

import com.example.demo.entity.Notification;
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
    @GetMapping public List<Notification> findAll() { return service.findAll(); }
    @GetMapping("/{id}") public Notification findById(@PathVariable Long id) { return service.findById(id); }
    @PostMapping public ResponseEntity<Notification> create(@RequestBody Notification notification) { Notification saved = service.create(notification); return ResponseEntity.created(linkTo(methodOn(NotificationController.class).findById(saved.getId())).toUri()).body(saved); }
    @PutMapping("/{id}") public Notification update(@PathVariable Long id, @RequestBody Notification notification) { return service.update(id, notification); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { service.delete(id); }
}
