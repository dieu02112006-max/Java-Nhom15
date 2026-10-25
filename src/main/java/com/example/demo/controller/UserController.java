package com.example.demo.controller;

import com.example.demo.entity.User;
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
    @GetMapping public List<User> findAll() { return service.findAll(); }
    @GetMapping("/{id}") public EntityModel<User> findById(@PathVariable Long id) { return EntityModel.of(service.findById(id), linkTo(methodOn(UserController.class).findById(id)).withSelfRel(), linkTo(methodOn(UserController.class).findAll()).withRel("users")); }
    @PostMapping public ResponseEntity<User> create(@RequestBody User user) { User saved = service.create(user); return ResponseEntity.created(linkTo(methodOn(UserController.class).findById(saved.getId())).toUri()).body(saved); }
    @PutMapping("/{id}") public User update(@PathVariable Long id, @RequestBody User user) { return service.update(id, user); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { service.delete(id); }
}
