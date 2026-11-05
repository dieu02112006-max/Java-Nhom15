package com.example.demo.controller;

import com.example.demo.dto.request.PackagePurchaseRequest;
import com.example.demo.dto.request.PackagePaymentReviewRequest;
import com.example.demo.dto.response.PackageCheckoutResponse;
import com.example.demo.dto.response.PackageOverviewResponse;
import com.example.demo.dto.response.PackagePaymentStatusResponse;
import com.example.demo.dto.response.PackagePlanResponse;
import com.example.demo.dto.response.PackageReviewResponse;
import com.example.demo.security.AppUserPrincipal;
import com.example.demo.service.ManualPackagePaymentService;
import com.example.demo.service.PackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackageController {
    private final PackageService service;
    private final ManualPackagePaymentService paymentService;

    @GetMapping
    public PackageOverviewResponse overview(@AuthenticationPrincipal AppUserPrincipal principal) {
        return service.overview(principal.id());
    }

    @GetMapping("/me")
    public PackagePlanResponse current(@AuthenticationPrincipal AppUserPrincipal principal) {
        return service.currentPlan(principal.id());
    }

    @PostMapping("/purchase")
    public PackagePlanResponse purchase(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody PackagePurchaseRequest request) {
        return service.purchase(principal.id(), request.planCode());
    }

    @PostMapping("/checkout")
    public PackageCheckoutResponse checkout(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody PackagePurchaseRequest request) {
        PackagePlanResponse plan = service.planByCode(request.planCode());
        return paymentService.createCheckout(principal.id(), plan, principal.fullName(), principal.getUsername());
    }

    @GetMapping("/orders/{txnRef}")
    public PackagePaymentStatusResponse paymentStatus(
            @AuthenticationPrincipal AppUserPrincipal principal, @PathVariable String txnRef) {
        return paymentService.getStatus(principal.id(), txnRef);
    }

    @GetMapping("/payment-review")
    public PackageReviewResponse paymentReview(@RequestParam String token) {
        return paymentService.getReview(token);
    }

    @PostMapping("/payment-review")
    public PackagePaymentStatusResponse reviewPayment(@Valid @RequestBody PackagePaymentReviewRequest request) {
        return paymentService.review(request.token(), request.paid());
    }
}
