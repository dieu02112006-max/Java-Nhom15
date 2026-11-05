package com.example.demo.dto.response;

import java.util.List;

public record PackageOverviewResponse(PackagePlanResponse currentPlan, List<PackagePlanResponse> packages,
                                      boolean paymentEmailEnabled) {}
