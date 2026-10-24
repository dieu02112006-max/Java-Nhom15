package com.example.demo.controller;

import com.example.demo.model.AttachmentModel;
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
    @GetMapping public List<AttachmentModel> findAll() { return service.findAll(); }
    @GetMapping("/{id}") public AttachmentModel findById(@PathVariable Long id) { return service.findById(id); }
    @PostMapping public ResponseEntity<AttachmentModel> create(@RequestBody AttachmentModel model) { AttachmentModel saved = service.create(model); return ResponseEntity.created(linkTo(methodOn(AttachmentController.class).findById(saved.id())).toUri()).body(saved); }
    @PutMapping("/{id}") public AttachmentModel update(@PathVariable Long id, @RequestBody AttachmentModel model) { return service.update(id, model); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { service.delete(id); }
}
