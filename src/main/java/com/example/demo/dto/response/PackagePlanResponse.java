package com.example.demo.dto.response;

import java.util.List;

public record PackagePlanResponse(String code, String name, long priceVnd, int rank, List<String> features) {}
