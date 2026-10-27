package com.example.demo.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.entity.User;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.security.AppUserPrincipal;
import com.example.demo.service.FinanceService;
import com.example.demo.service.FinanceService.CategoryOption;
import com.example.demo.service.FinanceService.WalletOption;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class FinanceControllerTest {

    private final FinanceService service = mock(FinanceService.class);
    private final FinanceController controller = new FinanceController(service);
    private final AppUserPrincipal principal = new AppUserPrincipal(User.builder()
            .id(42L)
            .email("user@example.com")
            .passwordHash("unused")
            .fullName("Test User")
            .role(UserRole.USER)
            .build());

    @Test
    void categoriesUseTheAuthenticatedUsersId() {
        List<CategoryOption> categories = List.of(new CategoryOption(1L, "Food", "#123456"));
        when(service.categories(42L, "expense")).thenReturn(categories);

        assertSame(categories, controller.categories(principal, "expense"));

        verify(service).categories(42L, "expense");
    }

    @Test
    void walletsUseTheAuthenticatedUsersId() {
        List<WalletOption> wallets = List.of(new WalletOption(1L, "Cash", BigDecimal.ZERO));
        when(service.wallets(42L)).thenReturn(wallets);

        assertSame(wallets, controller.wallets(principal));

        verify(service).wallets(42L);
    }
}
