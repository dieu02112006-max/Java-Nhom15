package com.example.demo.controller;

import com.example.demo.model.UserModel;
import com.example.demo.service.UserService;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service = service; }
    @GetMapping public List<UserModel> findAll() { return service.findAll(); }
    @GetMapping("/{id}") public EntityModel<UserModel> findById(@PathVariable Long id) { return EntityModel.of(service.findById(id), linkTo(methodOn(UserController.class).findById(id)).withSelfRel(), linkTo(methodOn(UserController.class).findAll()).withRel("users")); }
    @PostMapping public ResponseEntity<UserModel> create(@RequestBody UserModel model) { UserModel saved = service.create(model); return ResponseEntity.created(linkTo(methodOn(UserController.class).findById(saved.id())).toUri()).body(saved); }
    @PutMapping("/{id}") public UserModel update(@PathVariable Long id, @RequestBody UserModel model) { return service.update(id, model); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { service.delete(id); }
}
