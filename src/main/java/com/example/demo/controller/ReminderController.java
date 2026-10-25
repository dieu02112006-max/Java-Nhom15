package com.example.demo.controller;

import com.example.demo.entity.Reminder;
import com.example.demo.service.ReminderService;
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
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService service;

    public ReminderController(ReminderService service) {
        this.service = service;
    }

    @GetMapping
    public List<Reminder> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Reminder findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<Reminder> create(@RequestBody Reminder reminder) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(reminder));
    }

    @PutMapping("/{id}")
    public Reminder update(@PathVariable Long id, @RequestBody Reminder reminder) {
        return service.update(id, reminder);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
