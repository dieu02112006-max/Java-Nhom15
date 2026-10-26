package com.example.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank @Email String email,
        @Size(min = 6, max = 100) String password,   // bắt buộc khi tạo, bỏ trống khi sửa = giữ mật khẩu cũ
        @NotBlank String fullName,
        String avatarUrl,
        @Size(min = 3, max = 3) String currencyDefault,
        String timezone
) {}