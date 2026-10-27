package com.example.demo.controller;

import com.example.demo.dto.request.WalletRequest;
import com.example.demo.dto.response.WalletResponse;
import com.example.demo.service.WalletService;
import com.example.demo.security.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService service;
    private final AuthorizationService authorization;

    @PostMapping
    public ResponseEntity<EntityModel<WalletResponse>> create(@Valid @RequestBody WalletRequest req) {
        authorization.requireUserAccess(req.userId());
        EntityModel<WalletResponse> model = toModel(service.create(req));
        return ResponseEntity
                .created(model.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(model);
    }

    @GetMapping
    public CollectionModel<EntityModel<WalletResponse>> getByUser(@RequestParam Long userId) {
        authorization.requireUserAccess(userId);
        List<EntityModel<WalletResponse>> items = service.getByUser(userId).stream()
                .map(this::toModel).toList();
        return CollectionModel.of(items,
                linkTo(methodOn(WalletController.class).getByUser(userId)).withSelfRel());
    }

    @GetMapping("/{id}")
    public EntityModel<WalletResponse> getById(@PathVariable Long id) {
        WalletResponse wallet = service.getById(id);
        authorization.requireUserAccess(wallet.userId());
        return toModel(wallet);
    }

    @PutMapping("/{id}")
    public EntityModel<WalletResponse> update(@PathVariable Long id, @Valid @RequestBody WalletRequest req) {
        authorization.requireUserAccess(service.getById(id).userId());
        authorization.requireUserAccess(req.userId());
        return toModel(service.update(id, req));
    }

    // "xóa" = lưu trữ ví
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        authorization.requireUserAccess(service.getById(id).userId());
        service.archive(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<WalletResponse> toModel(WalletResponse w) {
        return EntityModel.of(w,
                linkTo(methodOn(WalletController.class).getById(w.id())).withSelfRel(),
                linkTo(methodOn(TransactionController.class).getByWallet(w.id())).withRel("transactions"),
                linkTo(methodOn(WalletController.class).getByUser(w.userId())).withRel("wallets"));
    }
}
