package com.example.demo.controller;

import com.example.demo.entity.Attachment;
import com.example.demo.service.AttachmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {
    private final AttachmentService service;
    public AttachmentController(AttachmentService service) { this.service = service; }
    @GetMapping public List<Attachment> findAll() { return service.findAll(); }
    @GetMapping("/{id}") public Attachment findById(@PathVariable Long id) { return service.findById(id); }
    @PostMapping public ResponseEntity<Attachment> create(@RequestBody Attachment attachment) { Attachment saved = service.create(attachment); return ResponseEntity.created(linkTo(methodOn(AttachmentController.class).findById(saved.getId())).toUri()).body(saved); }
    @PutMapping("/{id}") public Attachment update(@PathVariable Long id, @RequestBody Attachment attachment) { return service.update(id, attachment); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { service.delete(id); }
}
