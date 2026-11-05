package com.example.demo.dto.response;

public record PackageReviewResponse(String transactionRef, String planCode, long amountVnd,
                                    String status, String customerName) {}
