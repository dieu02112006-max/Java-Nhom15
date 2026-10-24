package com.example.demo.controller;

import com.example.demo.model.SkinModel;
import com.example.demo.service.SkinService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/skins")
public class SkinController {
    private final SkinService service;
    public SkinController(SkinService service) { this.service = service; }
    @GetMapping public List<SkinModel> findAll() { return service.findAll(); }
    @GetMapping("/{id}") public SkinModel findById(@PathVariable Long id) { return service.findById(id); }
    @PostMapping public ResponseEntity<SkinModel> create(@RequestBody SkinModel model) { SkinModel saved = service.create(model); return ResponseEntity.created(linkTo(methodOn(SkinController.class).findById(saved.id())).toUri()).body(saved); }
    @PutMapping("/{id}") public SkinModel update(@PathVariable Long id, @RequestBody SkinModel model) { return service.update(id, model); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { service.delete(id); }
}
