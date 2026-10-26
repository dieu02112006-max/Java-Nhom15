package com.example.demo.controller;

import com.example.demo.entity.Skin;
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
    @GetMapping public List<Skin> findAll() { return service.findAll(); }
    @GetMapping("/{id}") public Skin findById(@PathVariable Long id) { return service.findById(id); }
    @PostMapping public ResponseEntity<Skin> create(@RequestBody Skin skin) { Skin saved = service.create(skin); return ResponseEntity.created(linkTo(methodOn(SkinController.class).findById(saved.getId())).toUri()).body(saved); }
    @PutMapping("/{id}") public Skin update(@PathVariable Long id, @RequestBody Skin skin) { return service.update(id, skin); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { service.delete(id); }
}
