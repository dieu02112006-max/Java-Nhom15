package com.example.demo.dto.response;

public record PackagePaymentStatusResponse(String transactionRef, String planCode, long amountVnd, String status) {}
