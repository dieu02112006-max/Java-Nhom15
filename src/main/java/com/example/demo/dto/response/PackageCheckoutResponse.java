package com.example.demo.dto.response;

public record PackageCheckoutResponse(String transactionRef, String planName, long amountVnd,
                                      String qrImageUrl, String status, boolean emailSent) {}
